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
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaMetadata
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaUploadRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.time.format.DateTimeFormatter
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaModel
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaResponse
import com.truvideo.sdk.model.exceptions.TruvideoSdkException

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
      put("fileType", request.fileType.name)
      put("createdAt", formatDate(request.createdAt))
      put("updatedAt", formatDate(request.updatedAt))
      put("tags", JSONObject(request.tags.toMap() as Map<*, *>))
      put("metadata", JSONObject(request.metadata.toMap() as Map<*, *>))
      put("durationMilliseconds", request.durationMilliseconds?.toString() ?: "0")
      put("remoteId", request.mediaId ?: "")
      put("remoteURL", request.mediaUrl ?: "")
      put("transcriptionURL", request.transcriptionUrl ?: "")
      put("status", request.status.name)
      put("progress", (request.uploadProgress?.times(100) ?: 0))
    }.toString()
  }

  // ─── Stream Upload Map Helper ─────────────────────────────────────────────────

  private fun mapStreamUploadRequestToJson(request: TruvideoSdkMediaUploadRequest): JSONObject {
    val partsList = JSONArray()
    request.parts.forEach { part ->
      partsList.put(JSONObject().apply {
        put("index", part.index)
        put("createdAt", formatDate(part.createdAt))
        put("updatedAt", formatDate(part.updatedAt))
        put("startedAt", formatDate(part.metrics.startedAt))
        put("endedAt", formatDate(part.metrics.endedAt))
        put("isCompleted", part.metrics.completed)
      })
    }

    return JSONObject().apply {
      put("id", request.id.toString())
      put("title", request.title ?: "")
      put("status", request.status.name)
      put("type", request.type.name)
      put("progress", (request.progress * 100))
      put("thumbnailPath", request.thumbnailPath ?: "")
      put("mediaId", request.mediaId ?: "")
      put("tags", JSONObject(request.tags.toMap() as Map<*, *>))
      put("metadata", JSONObject(request.metadata.toMap() as Map<*, *>))
      put("includeInReport", request.includeInReport)
      put("isLibrary", request.isLibrary)
      // File upload metrics
      put("isStartOperationCompleted", request.fileUploadMetrics.completed)
      put("startOperationStartedAt", formatDate(request.fileUploadMetrics.startedAt))
      put("startOperationEndedAt", formatDate(request.fileUploadMetrics.endedAt))
      // Completion metrics
      put("isCompleteOperationCompleted", request.completionMetrics.completed)
      put("completeOperationStartedAt", formatDate(request.completionMetrics.startedAt))
      put("completeOperationEndedAt", formatDate(request.completionMetrics.endedAt))
      put("parts", partsList)
      put("createdAt", formatDate(request.createdAt))
      put("updatedAt", formatDate(request.updatedAt))
      put("startedAt", formatDate(request.fileUploadMetrics.startedAt))
      put("endedAt", formatDate(request.completionMetrics.endedAt))
    }
  }

  // ─── JSON → Tags / Metadata helpers ──────────────────────────────────────────

  private fun buildTagsFromJson(jsonStr: String): TruvideoSdkMediaTags {
    val entries = mutableListOf<TruvideoSdkMediaTags.Entry>()
    try {
      val json = JSONObject(jsonStr)
      json.keys().forEach { key ->
        entries.add(TruvideoSdkMediaTags.Entry(key, json.getString(key)))
      }
    } catch (_: JSONException) { }
    return TruvideoSdkMediaTags(entries)
  }

  private fun buildMetadataFromJson(jsonStr: String): TruvideoSdkMediaMetadata {
    val builder = TruvideoSdkMediaMetadata.builder()
    try {
      val json = JSONObject(jsonStr)
      json.keys().forEach { key ->
        builder.set(key, json.getString(key))
      }
    } catch (_: JSONException) { }
    return builder.build()
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

      try {
        val jsonTag = JSONObject(tag)
        val keys = jsonTag.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          builderObj.addTag(key, jsonTag.getString(key))
        }
      } catch (_: JSONException) { }

      try {
        val jsonMetadata = JSONObject(metaData)
        val metadataKeys = jsonMetadata.keys()
        while (metadataKeys.hasNext()) {
          val key = metadataKeys.next()
          builderObj.addMetadata(key, jsonMetadata.getString(key))
        }
      } catch (_: JSONException) { }

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
          val mainStatus: TruvideoSdkMediaFileUploadRequestStatus? = when (status.uppercase()) {
            "UPLOADING"     -> TruvideoSdkMediaFileUploadRequestStatus.UPLOADING
            "IDLE"          -> TruvideoSdkMediaFileUploadRequestStatus.IDLE
            "ERROR"         -> TruvideoSdkMediaFileUploadRequestStatus.ERROR
            "PAUSED"        -> TruvideoSdkMediaFileUploadRequestStatus.PAUSED
            "COMPLETED"     -> TruvideoSdkMediaFileUploadRequestStatus.COMPLETED
            "CANCELED"      -> TruvideoSdkMediaFileUploadRequestStatus.CANCELED
            "SYNCHRONIZING" -> TruvideoSdkMediaFileUploadRequestStatus.SYNCHRONIZING
            else            -> null
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
                  val metadataObj = JSONObject(response.metadata.toMap() as Map<*, *>)
                  val tagsObj = JSONObject(response.tags.toMap() as Map<*, *>)
                  val mainResponse = JSONObject().apply {
                    put("id", id)
                    put("createdDate", formatDate(response.createdAt))
                    put("remoteId", response.mediaId ?: "")
                    put("uploadedFileURL", response.mediaUrl ?: "")
                    put("metaData", metadataObj)
                    put("tags", tagsObj)
                    put("transcriptionURL", response.transcriptionUrl ?: "")
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

  // ─── createStreamUploadRequest ───────────────────────────────────────────────
  // This stub notifies the caller clearly rather than silently failing.

  override fun createStreamUploadRequest(filePath: String?, promise: Promise?) {
    promise!!.reject(
      "NOT_SUPPORTED",
      "Stream upload requests are created by the recording SDK, not by this method. " +
      "Use getAllStreamUploadRequests() to list pending requests after recording."
    )
  }
  // ─── getAllStreamUploadRequests ───────────────────────────────────────────────

  override fun getAllStreamUploadRequests(promise: Promise?) {
    scope.launch {
      try {
        val requests = TruvideoSdkMedia.getAllUploadRequests()
        val jsonArray = JSONArray()
        requests.forEach { request ->
          jsonArray.put(mapStreamUploadRequestToJson(request))
        }
        withContext(Dispatchers.Main) {
          promise!!.resolve(jsonArray.toString())
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── getStreamUploadRequestById ──────────────────────────────────────────────

  override fun getStreamUploadRequestById(id: String?, promise: Promise?) {
    val longId = id?.toLongOrNull()
    if (longId == null) {
      promise!!.reject("INVALID_ID", "Stream upload request ID must be a valid numeric (Long) value")
      return
    }
    scope.launch {
      try {
        val request = TruvideoSdkMedia.getUploadRequestById(longId)
        withContext(Dispatchers.Main) {
          if (request == null) {
            promise!!.resolve("{}")
          } else {
            promise!!.resolve(mapStreamUploadRequestToJson(request).toString())
          }
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── uploadStreamUploadRequest ───────────────────────────────────────────────

  override fun uploadStreamUploadRequest(
    id: String?,
    title: String?,
    tags: String?,
    metadata: String?,
    includeInReport: Boolean,
    isLibrary: Boolean,
    promise: Promise?
  ) {
    val longId = id?.toLongOrNull()
    if (longId == null) {
      promise!!.reject("INVALID_ID", "Stream upload request ID must be a valid numeric (Long) value")
      return
    }
    scope.launch {
      try {
        val request = TruvideoSdkMedia.getUploadRequestById(longId)
        if (request == null) {
          withContext(Dispatchers.Main) {
            promise!!.reject("NOT_FOUND", "Stream upload request not found for id: $id")
          }
          return@launch
        }
        val tagsObj = buildTagsFromJson(tags ?: "{}")
        val metadataObj = buildMetadataFromJson(metadata ?: "{}")
        request.upload(
          title = title ?: "",
          tags = tagsObj,
          metadata = metadataObj,
          includeInReport = includeInReport,
          isLibrary = isLibrary
        )
        withContext(Dispatchers.Main) {
          promise!!.resolve("Stream upload started")
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── pauseStreamUploadRequest ────────────────────────────────────────────────

  override fun pauseStreamUploadRequest(id: String?, promise: Promise?) {
    val longId = id?.toLongOrNull()
    if (longId == null) {
      promise!!.reject("INVALID_ID", "Stream upload request ID must be a valid numeric (Long) value")
      return
    }
    scope.launch {
      try {
        val request = TruvideoSdkMedia.getUploadRequestById(longId)
        if (request == null) {
          withContext(Dispatchers.Main) {
            promise!!.reject("NOT_FOUND", "Stream upload request not found for id: $id")
          }
          return@launch
        }
        request.pause()
        withContext(Dispatchers.Main) {
          promise!!.resolve("Stream paused")
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── resumeStreamUploadRequest ───────────────────────────────────────────────

  override fun resumeStreamUploadRequest(id: String?, promise: Promise?) {
    val longId = id?.toLongOrNull()
    if (longId == null) {
      promise!!.reject("INVALID_ID", "Stream upload request ID must be a valid numeric (Long) value")
      return
    }
    scope.launch {
      try {
        val request = TruvideoSdkMedia.getUploadRequestById(longId)
        if (request == null) {
          withContext(Dispatchers.Main) {
            promise!!.reject("NOT_FOUND", "Stream upload request not found for id: $id")
          }
          return@launch
        }
        request.resume()
        withContext(Dispatchers.Main) {
          promise!!.resolve("Stream resumed")
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── retryStreamUploadRequest ────────────────────────────────────────────────

  override fun retryStreamUploadRequest(id: String?, promise: Promise?) {
    val longId = id?.toLongOrNull()
    if (longId == null) {
      promise!!.reject("INVALID_ID", "Stream upload request ID must be a valid numeric (Long) value")
      return
    }
    scope.launch {
      try {
        val request = TruvideoSdkMedia.getUploadRequestById(longId)
        if (request == null) {
          withContext(Dispatchers.Main) {
            promise!!.reject("NOT_FOUND", "Stream upload request not found for id: $id")
          }
          return@launch
        }
        request.retry()
        withContext(Dispatchers.Main) {
          promise!!.resolve("Stream retried")
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
    }
  }

  // ─── deleteStreamUploadRequest ───────────────────────────────────────────────

  override fun deleteStreamUploadRequest(id: String?, promise: Promise?) {
    val longId = id?.toLongOrNull()
    if (longId == null) {
      promise!!.reject("INVALID_ID", "Stream upload request ID must be a valid numeric (Long) value")
      return
    }
    scope.launch {
      try {
        val request = TruvideoSdkMedia.getUploadRequestById(longId)
        if (request == null) {
          withContext(Dispatchers.Main) {
            promise!!.reject("NOT_FOUND", "Stream upload request not found for id: $id")
          }
          return@launch
        }
        request.delete()
        withContext(Dispatchers.Main) {
          promise!!.resolve("Stream deleted")
        }
      } catch (e: TruvideoSdkException) {
        withContext(Dispatchers.Main) {
          promise!!.reject("TruvideoSdkException", e.message)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          promise!!.reject("Exception", e.message)
        }
      }
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
          "PDF"   -> TruvideoSdkMediaFileType.DOCUMENT
          "IMAGE" -> TruvideoSdkMediaFileType.IMAGE
          else   -> null
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
          jsonArray.put(JSONObject().apply {
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
          })
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
