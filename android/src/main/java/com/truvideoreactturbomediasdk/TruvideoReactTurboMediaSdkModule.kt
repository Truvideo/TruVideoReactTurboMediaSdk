package com.truvideoreactturbomediasdk

import android.os.Build
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaCallback
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaFileUploadCallback
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileType
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadStatus
import com.truvideo.sdk.media.model.TruvideoSdkMediaTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import java.time.format.DateTimeFormatter

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
      val file = File(filePath!!)
      if(!file.exists()){
        promise!!.reject("File Exceptions","File not found")
      }else{
        scope.launch {
          builder(filePath,tag!!,metaData!!,promise!!)
        }
      }
//      CoroutineScope(Dispatchers.Main).launch {
//        builder(filePath,tag!!,metaData!!,promise!!)
//      }
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
          val mainResponse = returnRequest(request)
          promise!!.resolve(mainResponse)
        }
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }



  fun returnRequest(request : TruvideoSdkMediaFileUploadRequest) : String{
    return JSONObject().apply {
      put("id", request.id)
      put("filePath", request.filePath)
      put("fileType", request.type)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        put("createdAt", DateTimeFormatter.ISO_INSTANT.format(request.createdAt.toInstant()) )
        put("updateAt",DateTimeFormatter.ISO_INSTANT.format(request.updatedAt.toInstant()))
      }else{
        put("createdAt", request.createdAt )
        put("updateAt",request.updatedAt)
      }
      put("tags" , request.tags)
      put("metadata", request.metadata)
      put("durationMilliseconds", request.durationMilliseconds)
      put("remoteId", request.remoteId)
      put("remoteURL", request.remoteUrl)
      put("transcriptionURL", request.transcriptionUrl)
      put("transcriptionLength", request.transcriptionLength)
      put("status", request.status)
      put("progress", request.uploadProgress)
    }.toString()
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
        val typeData : TruvideoSdkMediaFileType = when (type) {
          "Video" -> {
            TruvideoSdkMediaFileType.Video
          }
          "AUDIO" -> {
            TruvideoSdkMediaFileType.AUDIO
          }
          "PDF" -> {
            TruvideoSdkMediaFileType.PDF
          }
          "Image" -> {
            TruvideoSdkMediaFileType.Picture
          }
          else -> {
            TruvideoSdkMediaFileType.All
          }
        }
        val jsonTag = JSONObject(tag!!)
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
        val jsonArray = JSONArray()

        response.data.forEach { item ->
          val metadataObj = JSONObject()
          item.metadata.map.keys.forEach { key ->
            metadataObj.put(key, item.metadata.map[key])
          }
          val tagsObj = JSONObject()
          item.tags.map.keys.forEach { key ->
            tagsObj.put(key, item.tags.map[key])
          }

          val jsonObject = JSONObject().apply {
            put("id", item.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              put("createdDate", DateTimeFormatter.ISO_INSTANT.format(item.createdDate.toInstant()))
            }else {
              put("createdDate", item.createdDate)
            }
            put("remoteId", item.id)
            put("uploadedFileURL", item.url)
            put("metaData", metadataObj) // assuming toJson() returns JSON string
            put("tags", tagsObj) // assuming toJson() returns JSON array string
            put("transcriptionURL", item.transcriptionUrl)
            put("transcriptionLength", item.transcriptionLength)
            put("fileType", item.type.name)
          }
          jsonArray.put(jsonObject)
        }

// Resolve with JSON string
        promise!!.resolve(jsonArray.toString())
      }
    }catch (e: Exception){
      promise!!.reject("Exception",e.message)
    }
  }


  suspend fun builder(filePath: String, tag : String, metaData : String, promise: Promise){
    // Create a file upload request builder
    try{
      val builder = TruvideoSdkMedia.FileUploadRequestBuilder(filePath)
      val jsonTag = JSONObject(tag)
      val keys = jsonTag.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val value= jsonTag.getString(key) // Can be any type: String, Integer, Boolean, etc.
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
      builder.build(object:
        TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest> {
        override fun onComplete(data: TruvideoSdkMediaFileUploadRequest) {
          val mainResponse = returnRequest(data)
          // Upload the file
          promise.resolve(mainResponse)
        }
        override fun onError(exception: TruvideoSdkException) {
          promise.reject("TruvideoSdkException",exception.message)
        }
      })
    }catch (e: Exception){
      promise.reject("Exception",e.message)
    }
  }

  override fun uploadMedia(id: String,promise: Promise){
    try{
        TruvideoSdkMedia.getFileUploadRequestById(id,object:
          TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest?> {
          override fun onComplete(data: TruvideoSdkMediaFileUploadRequest?) {
            if(data == null){
              promise.reject("File Exceptions","File Exceptions")
            }
            val file = File(data!!.filePath)
            if(!file.exists()){
              promise.reject("File Exceptions","File not found")
            }else{
              data.upload(object:
                TruvideoSdkMediaCallback<Unit> {
                override fun onComplete(data: Unit) {
                }
                override fun onError(exception: TruvideoSdkException) {
                  promise.reject("TruvideoSdkException",exception.message)
                }
              },object : TruvideoSdkMediaFileUploadCallback {
                override fun onComplete(id: String, response: TruvideoSdkMediaFileUploadRequest) {
                  // Handle completion
                  val metadataObj = JSONObject()
                  response.metadata.map.keys.forEach { key ->
                    metadataObj.put(key, response.metadata.map[key])
                  }
                  val tagsObj = JSONObject()
                  response.tags.map.keys.forEach { key ->
                    tagsObj.put(key, response.tags.map[key])
                  }
                  val mainResponse = JSONObject().apply {
                    put("id", id) // Generate a unique ID for the event
                    put("createdDate", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) DateTimeFormatter.ISO_INSTANT.format(response.createdAt.toInstant()) else response.createdAt)
                    put("remoteId", response.remoteId)
                    put("uploadedFileURL", response.remoteUrl)
                    put("metaData", metadataObj) // if toJson() is JSON string
                    put("tags", tagsObj) // or JSONArray if tags is a list
                    put("transcriptionURL", response.transcriptionUrl)
                    put("transcriptionLength", response.transcriptionLength)
                    put("fileType", response.type.name)
                  }
                  promise.resolve(mainResponse.toString())
                  sendEvent(reactApplicationContext,"onComplete",mainResponse.toString())
                }

                override fun onProgressChanged(id: String, progress: Float) {
                  // Handle progress

                  val mainResponse = JSONObject().apply {
                    put("id", id) // Generate a unique ID for the event
                    put("progress",  (progress*100))
                  }
                  sendEvent(reactApplicationContext,"onProgress",mainResponse.toString())
                }

                override fun onError(id: String, ex: TruvideoSdkException) {
                  // Handle error

                  val mainResponse = JSONObject().apply {
                    put("id", id) // Generate a unique ID for the event
                    put("error",  ex)
                  }
                  sendEvent(reactApplicationContext,"onError",mainResponse.toString())
                  promise.reject(id,ex.message,ex)
                }
              })
            }
          }
          override fun onError(exception: TruvideoSdkException) {
            promise.reject("TruvideoSdkException",exception.message)
          }
        })


    }catch (e: Exception){
      promise.reject("Exception",e.message)
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
