package com.truvideoreactturbomediasdk

import android.os.Build
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaCallback
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaFileUploadCallback
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileType
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequestStatus
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import truvideo.sdk.common.exceptions.TruvideoSdkException
import java.io.File
import java.time.format.DateTimeFormatter
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaModel
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaResponse

@ReactModule(name = TruvideoReactTurboMediaSdkModule.NAME)
class TruvideoReactTurboMediaSdkModule(reactContext: ReactApplicationContext) :
  NativeTruvideoReactTurboMediaSdkSpec(reactContext) {

  val scope = CoroutineScope(Dispatchers.Main)

  override fun getName(): String {
    return NAME
  }

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  // ─── Date Helper ────────────────────────────────────────────────────────────

  private fun formatDate(date: java.util.Date?): String? {
    if (date == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
    } else {
      date.toString()
    }
  }

  // ─── Map Helpers ─────────────────────────────────────────────────────────────

  fun returnRequestsJson(requests: List<TruvideoSdkMediaFileUploadRequest>): String {
    val jsonArray = JSONArray()
    for (request in requests) {
      jsonArray.put(JSONObject(returnRequest(request)))
    }
    return jsonArray.toString()
  }

  fun returnRequest(request: TruvideoSdkMediaFileUploadRequest): String {
    return JSONObject().apply {
      put("id", request.id)
      put("filePath", request.filePath)
      // FIX: was request.type — correct field is fileType
      put("fileType", request.fileType.name)
      put("createdAt", formatDate(request.createdAt))
      put("updatedAt", formatDate(request.updatedAt))
      // FIX: was request.tags / request.metadata directly — use .toMap()
      put("tags", JSONObject(request.tags.toMap() as Map<*, *>))
      put("metadata", JSONObject(request.metadata.toMap() as Map<*, *>))
      put("durationMilliseconds", request.durationMilliseconds?.toString() ?: "0")
      // FIX: was request.remoteId — correct field is mediaId
      put("remoteId", request.mediaId ?: "")
      // FIX: was request.remoteUrl — correct field is mediaUrl
      put("remoteURL", request.mediaUrl ?: "")
      put("transcriptionURL", request.transcriptionUrl ?: "")
      put("status", request.status.name)
      // FIX: multiply by 100 to get percentage, same as Flutter plugin
      put("progress", (request.uploadProgress?.times(100) ?: 0))
    }.toString()
  }

  // ─── mediaBuilder ────────────────────────────────────────────────────────────

  override fun mediaBuilder(filePath: String?, tag: String?, metaData: String?, promise: Promise?) {
    try {
      val file = File(filePath!!)
      if (!file.exists()) {
        promise!!.reject("File Exceptions", "File not found")
      } else {
        scope.launch {
          builder(filePath, tag!!, metaData!!, promise!!)
        }
      }
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  suspend fun builder(filePath: String, tag: String, metaData: String, promise: Promise) {
    try {
      val builderObj = TruvideoSdkMedia.FileUploadRequestBuilder(filePath)

      // Tags
      try {
        val jsonTag = JSONObject(tag)
        val keys = jsonTag.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          builderObj.addTag(key, jsonTag.getString(key))
        }
      } catch (_: JSONException) { }

      // Metadata
      try {
        val jsonMetadata = JSONObject(metaData)
        val metadataKeys = jsonMetadata.keys()
        while (metadataKeys.hasNext()) {
          val key = metadataKeys.next()
          builderObj.addMetadata(key, jsonMetadata.getString(key))
        }
      } catch (_: JSONException) { }

      // FIX: build() is now a suspend function — no callback needed
      val request = builderObj.build()
      withContext(Dispatchers.Main) {
        promise.resolve(returnRequest(request))
      }
    } catch (e: TruvideoSdkException) {
      withContext(Dispatchers.Main) {
        promise.reject("TruvideoSdkException", e.message)
      }
    } catch (e: Exception) {
      withContext(Dispatchers.Main) {
        promise.reject("Exception", e.message)
      }
    }
  }

  // ─── getFileUploadRequestById ────────────────────────────────────────────────

  override fun getFileUploadRequestById(id: String?, promise: Promise?) {
    try {
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        if (request == null) {
          promise!!.resolve("{}")
        } else {
          promise!!.resolve(returnRequest(request))
        }
      }
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  // ─── getAllFileUploadRequests ─────────────────────────────────────────────────

  override fun getAllFileUploadRequests(status: String?, promise: Promise?) {
    try {
      scope.launch {
        if (status.isNullOrEmpty()) {
          val requests = TruvideoSdkMedia.getAllFileUploadRequests()
          promise!!.resolve(returnRequestsJson(requests))
        } else {
          // Docs §5.6.2: correct enum is TruvideoSdkMediaFileUploadRequestStatus
          val mainStatus: TruvideoSdkMediaFileUploadRequestStatus? = when (status.uppercase()) {
            "UPLOADING" -> TruvideoSdkMediaFileUploadRequestStatus.UPLOADING
            "IDLE" -> TruvideoSdkMediaFileUploadRequestStatus.IDLE
            "ERROR" -> TruvideoSdkMediaFileUploadRequestStatus.ERROR
            "PAUSED" -> TruvideoSdkMediaFileUploadRequestStatus.PAUSED
            "COMPLETED" -> TruvideoSdkMediaFileUploadRequestStatus.COMPLETED
            "CANCELED" -> TruvideoSdkMediaFileUploadRequestStatus.CANCELED
            "SYNCHRONIZING" -> TruvideoSdkMediaFileUploadRequestStatus.SYNCHRONIZING
            else -> null
          }
          val requests = TruvideoSdkMedia.getAllFileUploadRequests(mainStatus)
          promise!!.resolve(returnRequestsJson(requests))
        }
      }
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  // ─── uploadMedia ─────────────────────────────────────────────────────────────

  override fun uploadMedia(id: String, promise: Promise) {
    try {
      TruvideoSdkMedia.getFileUploadRequestById(
        id,
        object : TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest?> {
          override fun onComplete(data: TruvideoSdkMediaFileUploadRequest?) {
            if (data == null) {
              promise.reject("File Exceptions", "Upload request not found")
              return
            }
            val file = File(data.filePath)
            if (!file.exists()) {
              promise.reject("File Exceptions", "File not found")
              return
            }
            data.upload(
              object : TruvideoSdkMediaCallback<Unit> {
                override fun onComplete(data: Unit) { }
                override fun onError(exception: TruvideoSdkException) {
                  promise.reject("TruvideoSdkException", exception.message)
                }
              },
              object : TruvideoSdkMediaFileUploadCallback {
                override fun onComplete(id: String, response: TruvideoSdkMediaFileUploadRequest) {
                  // FIX: use .toMap() for tags/metadata, correct field names
                  val metadataObj = JSONObject(
                    response.metadata.toMap() as Map<*, *>
                  )
                  val tagsObj = JSONObject(
                    response.tags.toMap() as Map<*, *>
                  )
                  val mainResponse = JSONObject().apply {
                    put("id", id)
                    put("createdDate", formatDate(response.createdAt))
                    // FIX: was response.remoteId → mediaId
                    put("remoteId", response.mediaId ?: "")
                    // FIX: was response.remoteUrl → mediaUrl
                    put("uploadedFileURL", response.mediaUrl ?: "")
                    put("metaData", metadataObj)
                    put("tags", tagsObj)
                    put("transcriptionURL", response.transcriptionUrl ?: "")
                    // FIX: was request.type → fileType
                    put("fileType", response.fileType.name)
                  }
                  promise.resolve(mainResponse.toString())
                  sendEvent(reactApplicationContext, "onComplete", mainResponse.toString())
                }

                override fun onProgressChanged(id: String, progress: Float) {
                  val mainResponse = JSONObject().apply {
                    put("id", id)
                    put("progress", progress * 100)
                  }
                  sendEvent(reactApplicationContext, "onProgress", mainResponse.toString())
                }

                override fun onError(id: String, ex: TruvideoSdkException) {
                  val mainResponse = JSONObject().apply {
                    put("id", id)
                    put("error", ex.message ?: "Unknown error")
                  }
                  sendEvent(reactApplicationContext, "onError", mainResponse.toString())
                  promise.reject(id, ex.message, ex)
                }
              }
            )
          }

          override fun onError(exception: TruvideoSdkException) {
            promise.reject("TruvideoSdkException", exception.message)
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("Exception", e.message)
    }
  }

  // ─── pauseMedia ───────────────────────────────────────────────────────────────

  override fun pauseMedia(id: String?, promise: Promise?) {
    try {
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        if (request == null) {
          promise!!.reject("ERROR", "Upload request not found")
          return@launch
        }
        when (request.status) {
          TruvideoSdkMediaFileUploadRequestStatus.UPLOADING -> {
            request.pause()
            promise!!.resolve("Pause Success")
          }
          TruvideoSdkMediaFileUploadRequestStatus.PAUSED -> {
            promise!!.reject("ALREADY_PAUSED", "Upload is already paused")
          }
          TruvideoSdkMediaFileUploadRequestStatus.COMPLETED -> {
            promise!!.reject("COMPLETED", "Upload is already completed")
          }
          else -> {
            promise!!.reject("INVALID_STATE", "Cannot pause. Current status: ${request.status}")
          }
        }
      }
    } catch (e: TruvideoSdkException) {
      promise!!.reject("TruvideoSdkException", e.message)
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  // ─── resumeMedia ─────────────────────────────────────────────────────────────

  override fun resumeMedia(id: String?, promise: Promise?) {
    try {
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        if (request == null) {
          promise!!.reject("ERROR", "Upload request not found")
          return@launch
        }
        when (request.status) {
          TruvideoSdkMediaFileUploadRequestStatus.PAUSED -> {
            request.resume()
            promise!!.resolve("Resume Success")
          }
          TruvideoSdkMediaFileUploadRequestStatus.UPLOADING -> {
            promise!!.reject("ALREADY_UPLOADING", "Upload is already in progress")
          }
          TruvideoSdkMediaFileUploadRequestStatus.COMPLETED -> {
            promise!!.reject("COMPLETED", "Upload is already completed")
          }
          else -> {
            promise!!.reject("INVALID_STATE", "Cannot resume. Current status: ${request.status}. Must be PAUSED.")
          }
        }
      }
    } catch (e: TruvideoSdkException) {
      promise!!.reject("TruvideoSdkException", e.message)
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  // ─── cancelMedia ─────────────────────────────────────────────────────────────

  override fun cancelMedia(id: String?, promise: Promise?) {
    try {
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        request!!.cancel()
        promise!!.resolve("Cancel Success")
      }
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  // ─── deleteMedia ─────────────────────────────────────────────────────────────

  override fun deleteMedia(id: String?, promise: Promise?) {
    try {
      scope.launch {
        val request = TruvideoSdkMedia.getFileUploadRequestById(id!!)
        request!!.delete()
        promise!!.resolve("Delete Success")
      }
    } catch (e: Exception) {
      promise!!.reject("Exception", e.message)
    }
  }

  // ─── search ──────────────────────────────────────────────────────────────────

  override fun search(
    tag: String?,
    type: String?,
    page: String?,
    pageSize: String?,
    promise: Promise?
  ) {
    scope.launch {
      try {
        val typeData: TruvideoSdkMediaFileType? = when (type?.uppercase()) {
          "VIDEO" -> TruvideoSdkMediaFileType.VIDEO
          "AUDIO" -> TruvideoSdkMediaFileType.AUDIO
          "PDF" -> TruvideoSdkMediaFileType.DOCUMENT
          "IMAGE" -> TruvideoSdkMediaFileType.IMAGE
          else -> null
        }

        val tagsList = mutableListOf<TruvideoSdkMediaTags.Entry>()
        try {
          val jsonTag = JSONObject(tag ?: "{}")
          val keys = jsonTag.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            tagsList.add(TruvideoSdkMediaTags.Entry(key, jsonTag.getString(key)))
          }
        } catch (_: JSONException) { }

        val resultData = TruvideoSdkMedia.search(
          tags = TruvideoSdkMediaTags(tagsList),
          type = typeData,
          page = page?.toIntOrNull() ?: 0,
          pageSize = pageSize?.toIntOrNull() ?: 10,
          isLibrary = null
        )

        val jsonArray = JSONArray()

        resultData?.data?.items?.forEach { item ->
          val metadataObj = JSONObject(item.metadata.toMap() as Map<*, *>)
          val tagsObj = JSONObject(item.tags.toMap() as Map<*, *>)

          val jsonObject = JSONObject().apply {
            put("id", item.id)
            put("createdDate", formatDate(item.createdAt))
            put("remoteId", item.id)
            put("uploadedFileURL", item.url)
            put("metaData", metadataObj)
            put("tags", tagsObj)
            put("transcriptionURL", item.transcriptionUrl)
            put("transcriptionLength", item.transcriptionLength)
            put("fileType", item.type.name)
            put("title", item.title)
            put("duration", item.duration ?: "0")
            put("isLibrary", item.isLibrary)
          }
          jsonArray.put(jsonObject)
        }

        val responseObject = JSONObject().apply {
          put("data", jsonArray)
          put("last", resultData?.data?.last)
          put("totalElements", resultData?.data?.totalElements)
          put("totalPages", resultData?.data?.totalPages)
          put("number", resultData?.data?.page)
          put("size", resultData?.data?.pageSize)
        }

        withContext(Dispatchers.Main) {
          promise!!.resolve(responseObject.toString())
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── searchById ──────────────────────────────────────────────────────────────
  // Docs §5.2.3: searchById(ids: List<String>, page, pageSize) → TruvideoSdkMediaPagedResult?

  override fun searchById(id: String, promise: Promise) {
    if (id.isEmpty()) {
      promise.reject("INVALID_ID", "Search ID cannot be empty")
      return
    }

    TruvideoSdkMedia.searchById(
      id = id,
      callback = object : TruvideoSdkMediaCallback<TruvideoSdkMediaResponse<TruvideoSdkMediaModel?>?> {
        override fun onComplete(data: TruvideoSdkMediaResponse<TruvideoSdkMediaModel?>?) {
          scope.launch {
            try {
              if (data == null) {
                withContext(Dispatchers.Main) {
                  promise.reject("NO_DATA", "No data found for ID: $id")
                }
                return@launch
              }

              val metadataObj = JSONObject().apply {
                data.data?.metadata?.toMap()?.forEach { (key, value) -> put(key, value) }
              }
              val tagsObj = JSONObject().apply {
                data.data?.tags?.toMap()?.forEach { (key, value) -> put(key, value) }
              }

              val jsonObject = JSONObject().apply {
                put("id", data.data?.id)
                put("createdDate", formatDate(data.data?.createdAt))
                put("remoteId", data.data?.id)
                put("uploadedFileURL", data.data?.url)
                put("metaData", metadataObj)
                put("tags", tagsObj)
                put("transcriptionURL", data.data?.transcriptionUrl)
                put("transcriptionLength", data.data?.transcriptionLength)
                put("fileType", data.data?.type?.name)
                put("title", data.data?.title)
                put("duration", data.data?.duration ?: "0")
              }

              withContext(Dispatchers.Main) {
                promise.resolve(jsonObject.toString())
              }
            } catch (e: Exception) {
              withContext(Dispatchers.Main) {
                promise.reject("SEARCH_BY_ID_ERROR", e.message ?: "Unknown error")
              }
            }
          }
        }

        override fun onError(exception: TruvideoSdkException) {
          scope.launch {
            withContext(Dispatchers.Main) {
              promise.reject("SEARCH_BY_ID_ERROR", exception.message ?: "Unknown error")
            }
          }
        }
      }
    )
  }

  // ─── Event emitter ───────────────────────────────────────────────────────────

  fun sendEvent(reactContext: ReactApplicationContext, eventName: String, data: String) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, data)
  }

  companion object {
    const val NAME = "TruvideoReactTurboMediaSdk"
  }
}
