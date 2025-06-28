package com.truvideoreactturbomediasdk

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.Gson
import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaCallback
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaFileUploadCallback
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileType
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadStatus
import com.truvideo.sdk.media.model.TruvideoSdkMediaMetadata
import com.truvideo.sdk.media.model.TruvideoSdkMediaTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import truvideo.sdk.common.exceptions.TruvideoSdkException

@ReactModule(name = TruvideoReactTurboMediaSdkModule.NAME)
class TruvideoReactTurboMediaSdkModule(reactContext: ReactApplicationContext) :
  NativeTruvideoReactTurboMediaSdkSpec(reactContext) {
  val scope = CoroutineScope(Dispatchers.Main)
  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  override fun mediaBuilder(filePath: String?, tag: String?, metaData: String?, promise: Promise?) {
    try {
      CoroutineScope(Dispatchers.Main).launch {
        builder(reactApplicationContext,filePath!!,tag!!,metaData!!,promise!!)
      }
    }catch (e : Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  override fun getFileUploadRequestById(id: String?, promise: Promise?) {
    try{
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        if(request == null){
          promise!!.resolve("{}")
        }else{
          var mainResponse = returnRequest(request)
          promise!!.resolve(mainResponse)
        }
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  fun returnRequest(request : TruvideoSdkMediaFileUploadRequest) : String{
    return Gson().toJson(
      mapOf<String, Any?>(
        "id" to request.id, // Generate a unique ID for the event
        "filePath" to request.filePath,
        "fileType" to request.type,
        "durationMilliseconds" to request.durationMilliseconds ,
        "remoteId" to request.remoteId ,
        "remoteURL" to request.remoteUrl,
        "transcriptionURL" to request.transcriptionUrl,
        "transcriptionLength" to request.transcriptionLength ,
        "status" to request.status,
        "progress" to request.uploadProgress
      )
    )
  }

  override fun getAllFileUploadRequests(status: String?, promise: Promise?) {
    try{
      scope.launch {
        if(status == ""){
          val request = TruvideoSdkMedia.getAllFileUploadRequests()
          promise!!.resolve(request)
        }else{
          val mainStatus : TruvideoSdkMediaFileUploadStatus? = when(status) {
            "UPLOADING" -> TruvideoSdkMediaFileUploadStatus.UPLOADING
            "IDLE" -> TruvideoSdkMediaFileUploadStatus.IDLE
            "ERROR" -> TruvideoSdkMediaFileUploadStatus.ERROR
            "PAUSED" -> TruvideoSdkMediaFileUploadStatus.PAUSED
            "COMPLETED" -> TruvideoSdkMediaFileUploadStatus.COMPLETED
            "CANCELED" -> TruvideoSdkMediaFileUploadStatus.CANCELED
            "SYNCHRONIZING" -> TruvideoSdkMediaFileUploadStatus.SYNCHRONIZING
            else -> null
          }
          val request = TruvideoSdkMedia.getAllFileUploadRequests(mainStatus)
          promise!!.resolve(request)
        }
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  override fun cancelMedia(id: String?, promise: Promise?) {
    try{
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        request!!.cancel()
        promise!!.resolve("Cancel Success")
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  override fun deleteMedia(id: String?, promise: Promise?) {
    try{
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        request!!.delete()
        promise!!.resolve("Delete Success")
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  override fun pauseMedia(id: String?, promise: Promise?) {
    try{
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        request!!.pause()
        promise!!.resolve("Pause Success")
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  override fun resumeMedia(id: String?, promise: Promise?) {
    try{
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        request!!.resume()
        promise!!.resolve("Resume Success")
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  override fun search(
    tag: String?,
    type: String?,
    page: String?,
    pageSize: String?,
    promise: Promise?
  ) {
    try{
      scope.launch {
        val typeData = when (type) {
          "All" -> {
            TruvideoSdkMediaFileType.All
          }
          "Video" -> {
            TruvideoSdkMediaFileType.Video
          }
          "AUDIO" -> {
            TruvideoSdkMediaFileType.AUDIO
          }
          "PDF" -> {
            TruvideoSdkMediaFileType.PDF
          }
          else -> {
            TruvideoSdkMediaFileType.Picture
          }
        }
        val jsonTag = JSONObject(tag)
        val map = mutableMapOf<String, String>()
        val keys = jsonTag.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val value= jsonTag.getString(key)
          map[key] = value
        }
        val response = TruvideoSdkMedia.search(
          tags = TruvideoSdkMediaTags(map),
          type = typeData,
          pageNumber = page!!.toInt(),
          pageSize = pageSize!!.toInt()
        )
        val gson = Gson()
        val list = ArrayList<String>()
        response.data.forEach {
          var mainResponse = gson.toJson(
            mapOf<String, Any?>(
              "id" to it.id, // Generate a unique ID for the event
              "createdDate" to it.createdDate,
              "remoteId" to it.id,
              "uploadedFileURL" to it.url,
              "metaData" to it.metadata.toJson(),
              "tags" to it.tags.toJson(),
              "transcriptionURL" to it.transcriptionUrl,
              "transcriptionLength" to  it.transcriptionLength,
              "fileType" to it.type.name
            )
          )
          list.add(mainResponse)
        }
        promise!!.resolve(gson.toJson(list))
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }


  suspend fun builder(context: Context, filePath: String, tag : String, metaData : String, promise: Promise){
    // Create a file upload request builder
    try{
      val builder = TruvideoSdkMedia.FileUploadRequestBuilder(filePath)
      val jsonTag = JSONObject(tag)
      var keys = jsonTag.keys()
      while (keys.hasNext()) {
        var key = keys.next()
        var value= jsonTag.getString(key) // Can be any type: String, Integer, Boolean, etc.
        builder.addTag(key, value)
      }
      // Metadata
      val jsonMetadata = JSONObject(metaData)
      val metadataKeys = jsonMetadata.keys()
      while (metadataKeys.hasNext()) {
        val key = metadataKeys.next()
        val value = jsonMetadata.getString(key) // Can be any type: String, Integer, Boolean, etc.
        builder.addMetadata(key, value)
      }
      // Build the request
      val request = builder.build()
      var mainResponse = returnRequest(request)
      // Upload the file
      promise.resolve(mainResponse)
    }catch (e: Exception){
      promise.reject("Exception",e.message)
    }
  }

  override fun uploadMedia(id: String,promise: Promise){
    try{
      scope.launch {
        val gson = Gson()
        val request = TruvideoSdkMedia.getFileUploadRequestById(id)
        request!!.upload(object : TruvideoSdkMediaFileUploadCallback {
          override fun onComplete(id: String, response: TruvideoSdkMediaFileUploadRequest) {
            // Handle completion
//        val mainResponse = mapOf<String, Any?>(
//          "id" to id,
//          "response" to response
//        )

            val mainResponse = mapOf<String, Any?>(
              "id" to id, // Generate a unique ID for the event
              "createdDate" to response.createdAt,
              "remoteId" to response.remoteId,
              "uploadedFileURL" to response.remoteUrl,
              "metaData" to response.metadata.toJson(),
              "tags" to response.tags.toJson(),
              "transcriptionURL" to response.transcriptionUrl,
              "transcriptionLength" to  response.transcriptionLength,
              "fileType" to response.type.name
            )
            promise.resolve(gson.toJson(mainResponse))
            sendEvent(reactApplicationContext,"onComplete",gson.toJson(mainResponse))
          }

          override fun onProgressChanged(id: String, progress: Float) {
            // Handle progress
            val mainResponse = mapOf<String, Any?>(
              "id" to id,
              "progress" to (progress*100)
            )
            sendEvent(reactApplicationContext,"onProgress",gson.toJson(mainResponse))
          }

          override fun onError(id: String, ex: TruvideoSdkException) {
            // Handle error
            val mainResponse = mapOf<String, Any?>(
              "id" to id,
              "error" to ex
            )
            promise.resolve(gson.toJson(mainResponse))
            sendEvent(reactApplicationContext,"onError",gson.toJson(mainResponse))
          }
        })
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }

  // broadcast event
  fun sendEvent(reactContext: ReactApplicationContext, eventName: String, progress: String) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, progress)
  }

  companion object {
    const val NAME = "TruvideoReactTurboMediaSdk"
  }
}
