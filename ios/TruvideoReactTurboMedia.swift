import Foundation
import Combine
import TruvideoSdkMedia
import React


@objc public class TruVideoReactMediaSdkClass: NSObject {
    private var disposeBag = Set<AnyCancellable>()
    @objc public func mediaBuilder(filePath: String, tag: String, metaData: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        guard let fileURL = URL(string: "file://\(filePath)") else {
            reject("INVALID_URL", "The file URL is invalid", nil)
            return
        }

        do {
            let builder = try createFileUploadRequestBuilder(fileURL: fileURL, tag: tag, metaData: metaData)
          var request = try builder.build()
          
          let dateFormatter = ISO8601DateFormatter()
          //let dateFormatter = DateFormatter()
          var tagString = ""
          let tagJsonData = try JSONSerialization.data(withJSONObject: request.tags.dictionary, options: [])
          if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
            tagString = tagJsonString
          }
          
          var metadataString = ""
          let metadataJsonData = try JSONSerialization.data(withJSONObject: request.metadata.dictionary, options: [])
          if let metadataJsonString = String(data: metadataJsonData, encoding: .utf8) {
            metadataString = metadataJsonString
          }
          
//          dateFormatter.dateFormat = "EEE MMM dd HH:mm:ss 'GMT'Z yyyy"
//          dateFormatter.locale = Locale(identifier: "en_US_POSIX")
          let mainResponse: [String: String] = [
            "id": request.id.uuidString, // Generate a unique ID for the event
            "filePath": request.filePath,
            "fileType": request.fileType.rawValue,
            "createdAt" : request.createdAt != nil ? dateFormatter.string(from: request.createdAt!) : "",
            "updatedAt" : request.updatedAt != nil ? dateFormatter.string(from: request.updatedAt!) : "",
            "tags" : tagString,
            "metadata" : metadataString,
            "durationMilliseconds":  "\(String(describing: request.durationMilliseconds))",
            "remoteId" : request.remoteId ?? "",
            "remoteURL" : request.remoteURL?.absoluteString ?? "",
            "transcriptionURL" : request.transcriptionURL ?? "",
            "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
            "status" : getStringStatus(status: request.status),
            "progress" : "\(request.uploadProgress)"
          ]

//          let mainResponse: [String: String] = [
//            "id": request.id.uuidString, // Generate a unique ID for the event
//            "filePath": request.filePath,
//            "fileType": request.fileType.rawValue,
//            "durationMilliseconds":  "\(String(describing: request.durationMilliseconds))",
//            "remoteId" : request.remoteId ?? "",
//            "remoteURL" : request.remoteURL?.absoluteString ?? "",
//            "transcriptionURL" : request.transcriptionURL ?? "",
//            "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
//            "status" : "\(request.status.rawValue)",
//            "progress" : "\(request.uploadProgress)"
//          ]
          let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])

            if let jsonString = String(data: jsonData, encoding: .utf8) {
                    print("mainResponse as JSON string: \(jsonString)")
                    resolve(jsonString) // Or wherever you need to use this JSON string
                } else {
                    print("Error: Could not convert JSON data to string.")
                    // Handle error: e.g., reject(error)
                }
            //try executeUploadRequest(builder: builder, resolve: resolve, reject: reject)
        } catch {
            reject("UPLOAD_ERROR", "Upload failed", error)
        }
    }

    private func createFileUploadRequestBuilder(fileURL: URL, tag: String, metaData: String) throws -> TruvideoSdkMedia.FileUploadRequestBuilder {
        let builder = TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)

        // Convert tag JSON string to dictionary
        let tagDict = try convertToDictionary(from: tag)
        for (key, value) in tagDict {
            builder.addTag(key, "\(value)")
        }

        // Convert metadata JSON string to Metadata type
        let metadataObj = try convertToDictionary(from: metaData)
        for (key, value) in metadataObj {
            builder.addMetadata(key, "\(value)")
        }
        return builder
    }

    private func executeUploadRequest(builder: TruvideoSdkMedia.FileUploadRequestBuilder, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) throws {
        let request =  try builder.build()

        // Print the file upload request for debugging
        print("fileUploadRequest: ", request.id.uuidString)

        // Completion of request
        let completeCancellable = request.completionHandler
            .receive(on: DispatchQueue.main)
            .sink(receiveCompletion: { receiveCompletion in
                switch receiveCompletion {
                case .finished:
                    print("Upload finished")
                case .failure(let error):
                    // Print any errors that occur during the upload process
                    print("Upload failure:", error)
                    reject("UPLOAD_ERROR", "Upload failed", error)
                  //Event().emit(name : "onError", body: error.localizedDescription)
                  self.sendEvent(withName: "onError", body: error.localizedDescription)
                }
            }, receiveValue: { uploadedResult in
                // Upon successful upload, retrieve the uploaded file URL
                let uploadedFileURL = uploadedResult.uploadedFileURL
                let metadataDict = uploadedResult.metadata
                let tags = uploadedResult.tags
                let transcriptionURL = uploadedResult.transcriptionURL
                let transcriptionLength = uploadedResult.transcriptionLength
                let id = request.id.uuidString
              print("uploadedResult: ", uploadedResult)

                print("tags: " , tags.dictionary)
                print("metaData: " , metadataDict.dictionary)
              
                // Send completion event
                let mainResponse: [String: Any] = [
                    "id": id, // Generate a unique ID for the event
                    "uploadedFileURL": uploadedFileURL.absoluteString,
                    "metaData": metadataDict.dictionary,
                    "tags": tags.dictionary,
                    "transcriptionURL": transcriptionURL?.absoluteString ?? "",
                    "transcriptionLength": transcriptionLength
                ]

                // resolve
                resolve(["status": mainResponse])
                do {
                  let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])

                  if let jsonString = String(data: jsonData, encoding: .utf8) {
                    //Event().emit(name: "onComplete", body: jsonString)
                    self.sendEvent(withName: "onComplete", body: jsonString)
                  }
                }catch{
                  
                }
            })

        // Store the completion handler in the dispose bag to avoid premature deallocation
        completeCancellable.store(in: &disposeBag)

        // Progress of request
        let progress = request.progressHandler
            .receive(on: DispatchQueue.main)
            .sink(receiveValue: { progress in
                let mainResponse: [String: Any] = [
                    "id": UUID().uuidString, // Generate a unique ID for the event
                    "progress": String(format: " %.2f %", progress.percentage * 100)
                ]
              do {
                let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])

                if let jsonString = String(data: jsonData, encoding: .utf8) {
                  //Event().emit(name: "onProgress", body: jsonString)
                  self.sendEvent(withName: "onProgress", body: jsonString)
                }
              }catch{
                
              }
            })

        // Store the progress handler in the dispose bag to avoid premature deallocation
        progress.store(in: &disposeBag)

        try request.upload()
    }

  @objc public func getFileUploadRequestById(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    do {
      let request =  try TruvideoSdkMedia.getFileUploadRequest(withId : id)
      let dateFormatter = ISO8601DateFormatter()
      //let dateFormatter = DateFormatter()
      var tagString = ""
      let tagJsonData = try JSONSerialization.data(withJSONObject: request.tags.dictionary, options: [])
      if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
        tagString = tagJsonString
      }
      
      var metadataString = ""
      let metadataJsonData = try JSONSerialization.data(withJSONObject: request.metadata.dictionary, options: [])
      if let metadataJsonString = String(data: metadataJsonData, encoding: .utf8) {
        metadataString = metadataJsonString
      }
      
//      dateFormatter.dateFormat = "EEE MMM dd HH:mm:ss 'GMT'Z yyyy"
//      dateFormatter.locale = Locale(identifier: "en_US_POSIX")
      let mainResponse: [String: String] = [
        "id": request.id.uuidString, // Generate a unique ID for the event
        "filePath": request.filePath,
        "fileType": request.fileType.rawValue,
        "createdAt" : request.createdAt != nil ? dateFormatter.string(from: request.createdAt!) : "",
        "updatedAt" : request.updatedAt != nil ? dateFormatter.string(from: request.updatedAt!) : "",
        "tags" : tagString,
        "metadata" : metadataString,
        "durationMilliseconds":  "\(String(describing: request.durationMilliseconds))",
        "remoteId" : request.remoteId ?? "",
        "remoteURL" : request.remoteURL?.absoluteString ?? "",
        "transcriptionURL" : request.transcriptionURL ?? "",
        "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
        "status" : getStringStatus(status: request.status),
        "progress" : "\(request.uploadProgress)"
      ]
      let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])

        if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("mainResponse as JSON string: \(jsonString)")
                resolve(jsonString) // Or wherever you need to use this JSON string
            } else {
                print("Error: Could not convert JSON data to string.")
                // Handle error: e.g., reject(error)
            }
        //try executeUploadRequest(builder: builder, resolve: resolve, reject: reject)
    } catch {
        resolve("{}")
    }

    //TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
  }
  
  @objc public func getAllFileRequests(status: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    do {
      var statusData : TruvideoSdkMediaUploadRequest.Status?
      if status == "COMPLETED" {
        statusData = .completed
      } else if status == "CANCELED" {
        statusData = .cancelled
      }else if status == "PAUSED" {
        statusData = .paused
      }else if status == "SYNCHRONIZING" {
        statusData = .synchronizing
      }else if status == "IDLE" {
        statusData = .idle
      }else if status == "UPLOADING" {
        statusData = .processing
      }else if status == "ERROR" {
        statusData = .error
      }else {
        statusData = nil
      }
      let requests =  try TruvideoSdkMedia.getFileUploadRequests(byStatus: statusData)
      //let dateFormatter = DateFormatter()
      let dateFormatter = ISO8601DateFormatter()
      var responseArray: [[String: String]] = []

          for request in requests {
              var tagString = ""
              let tagJsonData = try JSONSerialization.data(withJSONObject: request.tags.dictionary, options: [])
              if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
                tagString = tagJsonString
              }

              var metadataString = ""
              let metadataJsonData = try JSONSerialization.data(withJSONObject: request.metadata.dictionary, options: [])
              if let metadataJsonString = String(data: metadataJsonData, encoding: .utf8) {
                metadataString = metadataJsonString
              }

              let mainResponse: [String: String] = [
                  "id": request.id.uuidString,
                  "filePath": request.filePath,
                  "fileType": request.fileType.rawValue,
                  "createdAt": request.createdAt != nil ? dateFormatter.string(from: request.createdAt!) : "",
                  "updatedAt": request.updatedAt != nil ? dateFormatter.string(from: request.updatedAt!) : "",
                  "tags": tagString,
                  "metadata": metadataString,
                  "durationMilliseconds": "\(String(describing: request.durationMilliseconds))",
                  "remoteId": request.remoteId ?? "",
                  "remoteURL": request.remoteURL?.absoluteString ?? "",
                  "transcriptionURL": request.transcriptionURL ?? "",
                  "transcriptionLength": "\(String(describing: request.transcriptionLength))",
                  "status": getStringStatus(status: request.status),
                  "progress": "\(request.uploadProgress)"
              ]

              responseArray.append(mainResponse)
          }

          let jsonData = try JSONSerialization.data(withJSONObject: responseArray, options: [])
          if let jsonString = String(data: jsonData, encoding: .utf8) {
              print("responseArray as JSON string: \(jsonString)")
              resolve(jsonString) // return the whole array JSON string
          } else {
              print("Error: Could not convert JSON data to string.")
              // reject(error) or handle appropriately
          }

    } catch {
        resolve("{}")
    }

    //TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
  }

  func getStringStatus(status : TruvideoSdkMedia.TruvideoSdkMediaUploadRequest.Status?) -> String{
    return switch status {
    case .idle:
        "IDLE"
    case .completed:
        "COMPLETED"
    case .cancelled:
        "CANCELED"
    case .paused:
        "PAUSED"
    case .synchronizing:
        "SYNCHRONIZING"
    case .processing:
        "UPLOADING"
    case .error:
        "ERROR"
    case .none:
      "IDLE"
    @unknown default:
      ""
    }
  }
  
  @objc public func cancelMedia(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.cancel()
    resolve("Cancel Success")
  }

  @objc public func deleteMedia(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.delete()
    resolve("Delete Success")
  }

  @objc public func pauseMedia(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.pause()
    resolve("Pause Success")
  }

  @objc public func resumeMedia(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.resume()
    resolve("Resume Success")
  }

  @objc public func search(tag: String,type : String,page : String,pageSize : String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){

    let tagDict = try? convertToDictionary(from: tag)
    var tagBuild = TruvideoSdkMediaTags.builder()
    for (key, value) in tagDict! {
      var set = tagBuild.set(key, "\(value)")
    }
    let normalizedType = type.uppercased()
    var typeData : TruvideoSdkMediaType?
    if(normalizedType == "IMAGE"){
      typeData = .image
    }else if(normalizedType == "VIDEO"){
      typeData = .video
    }else if(normalizedType == "AUDIO"){
      typeData = .audio
    }else if(normalizedType == "PDF"){
      typeData = .document
    }else{
      typeData = nil
    }
    Task{
      let request = try? await TruvideoSdkMedia.search(type: typeData, tags: tagBuild.build(), pageNumber: Int(page) ?? 0, size: Int(pageSize) ?? 0)
      var mediaList: [TruvideoSDKMedia]? = request?.content
      if(mediaList == nil){
        let responseObject: [String: Any] = [
          "data": [],
          "last": true,
          "totalElements": 0,
          "totalPages": 0,
          "number": 0,
          "size": Int(pageSize) ?? 0
        ]
        if let jsonData = try? JSONSerialization.data(withJSONObject: responseObject, options: []),
           let jsonString = String(data: jsonData, encoding: .utf8) {
          resolve(jsonString)
        } else {
          resolve("{\"data\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"size\":0}")
        }
      }else{
        var list = [[String: Any]]()
        let dateFormatter = ISO8601DateFormatter()
//        let dateFormatter = DateFormatter()
//        dateFormatter.dateFormat = "EEE MMM dd HH:mm:ss 'GMT'Z yyyy"
//        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
        for media in mediaList! {
          let mediaDict: [String: Any] = [
            "id": media.remoteId,
            "createdDate": dateFormatter.string(from: media.createdDate),
            "remoteId": media.remoteId,
            "uploadedFileURL": media.uploadedFileURL.absoluteString,
            "metaData": media.metadata.dictionary,
            "tags": media.tags.dictionary,
            "transcriptionURL": media.transcriptionURL?.absoluteString ?? "",
            "transcriptionLength": "\(media.transcriptionLength)",
            "fileType": media.type.rawValue,
            "thumbnailUrl": media.thumbnailUrl?.absoluteString ?? "",
            "previewUrl": media.previewUrl?.absoluteString ?? ""
          ]
          list.append(mediaDict)
        }
        let responseObject: [String: Any] = [
          "data": list,
          "last": request?.last ?? true,
          "totalElements": request?.totalElements ?? list.count,
          "totalPages": request?.totalPages ?? (list.isEmpty ? 0 : 1),
          "number": request?.number ?? 0,
          "size": request?.size ?? list.count
        ]
        let jsonData = try JSONSerialization.data(withJSONObject: responseObject, options: [])
        if let jsonString = String(data: jsonData, encoding: .utf8) {
          resolve(jsonString)
        }else{
          reject("ERROR","JSON_ERROR",nil)
        }

      }


    }
    //try? request?.resume()
  }


  @objc public func uploadMedia(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)

    // Print the file upload request for debugging
    //print("fileUploadRequest: ", request.id.uuidString)

    // Completion of request
    let completeCancellable = request?.completionHandler
        .receive(on: DispatchQueue.main)
        .sink(receiveCompletion: { receiveCompletion in
            switch receiveCompletion {
            case .finished:
                print("Upload finished")
            case .failure(let error):
                // Print any errors that occur during the upload process
                print("Upload failure:", error)
                reject("UPLOAD_ERROR", "Upload failed", error)
            }
        }, receiveValue: { uploadedResult in
            // Upon successful upload, retrieve the uploaded file URL
            let uploadedFileURL = uploadedResult.uploadedFileURL
            let metadataDict = uploadedResult.metadata
            let tags = uploadedResult.tags
            let transcriptionURL = uploadedResult.transcriptionURL
            let transcriptionLength = uploadedResult.transcriptionLength
            let id = request?.id.uuidString
          print("uploadedResult: ", uploadedResult)

            print("tags: " , tags.dictionary)
            print("metaData: " , metadataDict.dictionary)
            // Send completion event
            let dateFormatter = ISO8601DateFormatter()

//          let dateFormatter = DateFormatter()
//          dateFormatter.dateFormat = "EEE MMM dd HH:mm:ss 'GMT'Z yyyy"
//          dateFormatter.locale = Locale(identifier: "en_US_POSIX")

          do {
            let tagJsonData = try JSONSerialization.data(withJSONObject: tags.dictionary, options: [])
            if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
              let mainResponse: [String: Any] = [
                  "id": id ?? "", // Generate a unique ID for the event
                  "createdDate" : dateFormatter.string(from: uploadedResult.createdDate),
                  "remoteId" : uploadedResult.remoteId,
                  "uploadedFileURL": uploadedFileURL.absoluteString,
                  "metaData": metadataDict.dictionary,
                  "tags":  tags.dictionary,
                  "transcriptionURL": transcriptionURL?.absoluteString ?? "",
                  "transcriptionLength": "\(transcriptionLength)",
                  "fileType" : uploadedResult.type.rawValue,
              ]
              let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])

                if let jsonString = String(data: jsonData, encoding: .utf8) {
                        print("mainResponse as JSON string: \(jsonString)")
                        resolve(jsonString) // Or wherever you need to use this JSON string
                  //Event().emit(name: "onComplete", body: jsonString)
                  self.sendEvent(withName: "onComplete", body: jsonString)
                    } else {

                        print("Error: Could not convert JSON data to string.")
                      reject("INVALID_JSON", "Error: Could not convert JSON data to string", nil)
                        // Handle error: e.g., reject(error)
                    }
            }else {
              reject("INVALID_JSON", "Error: Could not convert JSON data to string", nil)
            }

          }catch{
            reject("INVALID_JSON", "Error: Could not convert JSON data to string", nil)
          }

        })

    // Store the completion handler in the dispose bag to avoid premature deallocation
    completeCancellable?.store(in: &disposeBag)

    // Progress of request
    let progress = request?.progressHandler
        .receive(on: DispatchQueue.main)
        .sink(receiveValue: { progress in
            let mainResponse: [String: String] = [
                "id": id, // Generate a unique ID for the event
                "progress": String(format: " %.2f %", progress.percentage * 100)
            ]
          do{
            let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])
            if let jsonString = String(data: jsonData, encoding: .utf8) {
              //Event().emit(name: "onProgress", body: jsonString)
              self.sendEvent(withName: "onProgress", body: jsonString)
            }else{
              //Event().emit(name: "onProgress", body: "Unable to Parse JSON")
              self.sendEvent(withName: "onProgress", body: "Unable to Parse JSON")
            }
          }catch{
            //Event().emit(name: "onProgress", body: "Unable to Parse JSON")
            self.sendEvent(withName: "onProgress", body: "Unable to Parse JSON")
          }
        })

    // Store the progress handler in the dispose bag to avoid premature deallocation
    progress?.store(in: &disposeBag)

    try? request?.upload()
  }

    private func convertToDictionary(from jsonString: String) throws -> [String: Any] {
        guard let jsonData = jsonString.data(using: .utf8) else {
            throw NSError(domain: "Invalid JSON string", code: 0, userInfo: nil)
        }

        guard let jsonObject = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any] else {
            throw NSError(domain: "Invalid JSON format", code: 1, userInfo: nil)
        }

        return jsonObject
    }

  private func convertToJsonString(from dictionary: [String: Any]) throws -> String {
      let jsonData = try JSONSerialization.data(withJSONObject: dictionary, options: [])

      guard let jsonString = String(data: jsonData, encoding: .utf8) else {
          throw NSError(domain: "Unable to encode JSON string", code: 2, userInfo: nil)
      }

      return jsonString
  }

    // ────────────────────────────────────────────────────────────────────────────
    // STREAM UPLOAD — based on actual TruvideoSdkMediaInterface
    // ────────────────────────────────────────────────────────────────────────────

    @objc public func createStreamUploadRequest(
        _ filePath: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        guard let fileURL = URL(string: "file://\(filePath)") else {
            reject("INVALID_URL", "The file URL is invalid", nil)
            return
        }
        Task {
            do {
                let request = try await TruvideoSdkMedia.createUploadRequest(from: fileURL)
                let dict = mapStreamRequestToDict(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict)
                resolve(String(data: jsonData, encoding: .utf8) ?? "{}")
            } catch {
                reject("CREATE_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func getAllStreamUploadRequests(
        _ resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let requests = try await TruvideoSdkMedia.getAllUploadRequests()
                let list = requests.map { mapStreamRequestToDict($0) }
                let jsonData = try JSONSerialization.data(withJSONObject: list)
                resolve(String(data: jsonData, encoding: .utf8) ?? "[]")
            } catch {
                resolve("[]")
            }
        }
    }

    @objc public func getStreamUploadRequestById(
        _ id: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                let dict = mapStreamRequestToDict(request)
                if let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                   let jsonString = String(data: jsonData, encoding: .utf8) {
                    resolve(jsonString)
                } else {
                    resolve("{}")
                }
            } catch {
                resolve("{}")
            }
        }
    }
    @objc public func uploadStreamUploadRequest(
        _ id: String,
        title: String,
        tags: String,
        metadata: String,
        includeInReport: Bool,
        isLibrary: Bool,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                let status = normalizeStreamRequestStatus(request.status)

                if ["PROCESSING", "PAUSED", "COMPLETED"].contains(status) {
                    let dict = mapStreamRequestToDict(request)
                    if let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                       let jsonString = String(data: jsonData, encoding: .utf8) {
                        resolve(jsonString)
                    } else {
                        resolve("{}")
                    }
                    return
                }

                // Build tags from JSON string
                let tagsDict = (try? convertToDictionary(from: tags)) ?? [:]
                var tagsBuilder = TruvideoSdkMediaTags.builder()
                for (key, value) in tagsDict {
                    tagsBuilder = tagsBuilder.set(key, "\(value)")
                }

                // Build metadata from JSON string
                let metadataDict = (try? convertToDictionary(from: metadata)) ?? [:]
                let metadataBuilder = TruvideoSdkMediaMetadata.builder()
                for (key, value) in metadataDict {
                    _ = metadataBuilder.set(key, "\(value)")
                }

                // Use the Options struct — this is the correct iOS API
                let options = TruvideoSdkMediaStreamRequest.Options(
                    isIncludedInReport: includeInReport,
                    isLibrary: isLibrary,
                    metadata: metadataBuilder.build(),
                    tags: tagsBuilder.build().dictionary,
                    title: title
                )

                try request.upload(with: options)
                let initialResponse = mapStreamRequestToDict(request)
                if let jsonData = try? JSONSerialization.data(withJSONObject: initialResponse),
                   let jsonString = String(data: jsonData, encoding: .utf8) {
                    resolve(jsonString)
                } else {
                    resolve("{}")
                }

                let completeCancellable = request.completionHandler
                    .receive(on: DispatchQueue.main)
                    .sink(receiveCompletion: { completion in
                        switch completion {
                        case .finished:
                            break
                        case .failure(let error):
                            self.sendEvent(withName: "onError", body: error.localizedDescription)
                        }
                    }, receiveValue: { remoteId in
                        let responseDict: [String: Any] = [
                            "id": id,
                            "remoteId": remoteId,
                            "status": "uploaded"
                        ]
                        if let jsonData = try? JSONSerialization.data(withJSONObject: responseDict),
                           let jsonString = String(data: jsonData, encoding: .utf8) {
                            self.sendEvent(withName: "onComplete", body: jsonString)
                        }
                    })
                completeCancellable.store(in: &disposeBag)

            } catch {
                reject("STREAM_UPLOAD_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func pauseStreamUploadRequest(
        _ id: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.pause()
                resolve("Stream paused")
            } catch {
                reject("STREAM_PAUSE_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func resumeStreamUploadRequest(
        _ id: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.resume()
                resolve("Stream resumed")
            } catch {
                reject("STREAM_RESUME_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func retryStreamUploadRequest(
        _ id: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.retry()
                resolve("Stream retried")
            } catch {
                reject("STREAM_RETRY_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func deleteStreamUploadRequest(
        _ id: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.delete()
                resolve("Stream deleted")
            } catch {
                reject("STREAM_DELETE_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func searchById(
        _ id: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task {
            do {
                // getById is the correct method per swiftinterface
                guard let media = try await TruvideoSdkMedia.getById(id) else {
                    resolve("{}")
                    return
                }
                let dateFormatter = ISO8601DateFormatter()
                let tagJsonData = try JSONSerialization.data(withJSONObject: media.tags.dictionary)
                let tagString = String(data: tagJsonData, encoding: .utf8) ?? "{}"

                let metaJsonData = try JSONSerialization.data(withJSONObject: media.metadata.dictionary)
                let metaString = String(data: metaJsonData, encoding: .utf8) ?? "{}"

                let dict: [String: Any] = [
                    "id": media.remoteId,
                    "createdDate": dateFormatter.string(from: media.createdDate),
                    "remoteId": media.remoteId,
                    "uploadedFileURL": media.uploadedFileURL.absoluteString,
                    "metaData": metaString,
                    "tags": tagString,
                    "transcriptionURL": media.transcriptionURL?.absoluteString ?? "",
                    "transcriptionLength": "\(media.transcriptionLength)",
                    "fileType": media.type.rawValue,
                    "thumbnailUrl": media.thumbnailUrl?.absoluteString ?? "",
                    "previewUrl": media.previewUrl?.absoluteString ?? ""
                ]
                let jsonData = try JSONSerialization.data(withJSONObject: dict)
                resolve(String(data: jsonData, encoding: .utf8) ?? "{}")
            } catch {
                reject("SEARCH_BY_ID_ERROR", error.localizedDescription, error)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper — maps TruvideoSdkMediaStreamRequest to Dict
    // ────────────────────────────────────────────────────────────────────────────
    private func normalizeStreamRequestStatus(_ status: TruvideoSdkMediaStreamRequest.Status) -> String {
        switch status {
        case .cancelled:
            return "CANCELED"
        case .error:
            return "ERROR"
        case .paused:
            return "PAUSED"
        case .pending:
            return "IDLE"
        case .processing:
            return "PROCESSING"
        case .uploaded:
            return "COMPLETED"
        }
    }

    private func mapStreamRequestToDict(_ request: TruvideoSdkMediaStreamRequest) -> [String: Any] {
        let dateFormatter = ISO8601DateFormatter()
        return [
            "id": request.id.uuidString,
            "status": normalizeStreamRequestStatus(request.status),
            "type": request.fileType.rawValue,
            "progress": 0,
            "thumbnailPath": "",
            "mediaId": request.remoteId ?? "",
            "createdAt": dateFormatter.string(from: request.createdAt),
            "updatedAt": dateFormatter.string(from: request.createdAt),
            "filePath": request.fileUrl.absoluteString,
            "fileUrl": request.fileUrl.absoluteString,
            "isLibrary": request.isLibrary,
            "includeInReport": request.isIncludedInReport,
            "isIncludedInReport": request.isIncludedInReport,
            "tags": request.tags.dictionary,
            "metadata": request.metadata.dictionary,
            "durationMilliseconds": request.durationMilliseconds ?? 0,
            "parts": [],
        ]
    }
    
    // ────────────────────────────────────────────────────────────────────────────
    // AsyncStream bridges — push live status updates to JS via events
    // ────────────────────────────────────────────────────────────────────────────

    // Holds running stream tasks so we can cancel them
    private var streamAllTask: Task<Void, Never>? = nil
    private var streamByIdTasks: [String: Task<Void, Never>] = [:]

    @objc public func startStreamAllUploadRequests() {
        streamAllTask?.cancel()
        streamAllTask = Task {
            let stream = TruvideoSdkMedia.streamAllUploadRequests()
            for await requests in stream {
                if Task.isCancelled { break }
                let list = requests.map { mapStreamRequestToDict($0) }
                if let jsonData = try? JSONSerialization.data(withJSONObject: list),
                   let jsonString = String(data: jsonData, encoding: .utf8) {
                    sendEvent(withName: "onStreamAllUploadRequests", body: jsonString)
                }
            }
        }
    }

    @objc public func stopStreamAllUploadRequests() {
        streamAllTask?.cancel()
        streamAllTask = nil
    }

    @objc public func startStreamUploadRequestById(_ id: String) {
        streamByIdTasks[id]?.cancel()
        streamByIdTasks[id] = Task {
            let stream = TruvideoSdkMedia.streamUploadRequestById(id)
            for await request in stream {
                if Task.isCancelled { break }
                let dict = mapStreamRequestToDict(request)
                if let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                   let jsonString = String(data: jsonData, encoding: .utf8) {
                    sendEvent(withName: "onStreamUploadRequestById", body: jsonString)
                }
            }
        }
    }

    @objc public func stopStreamUploadRequestById(_ id: String) {
        streamByIdTasks[id]?.cancel()
        streamByIdTasks[id] = nil
    }
//    private func convertToMetadata(from jsonString: String) throws -> Metadata {
//        guard let jsonData = jsonString.data(using: .utf8) else {
//            throw NSError(domain: "Invalid JSON string", code: 0, userInfo: nil)
//        }
//
//        guard let metadataDict = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any] else {
//            throw NSError(domain: "Invalid JSON format", code: 0, userInfo: nil)
//        }
//
//        return convertToMetadata(metadataDict)
//    }
//
//    private func convertToMetadata(_ dict: [String: Any]) -> Metadata {
//        var metadata = Metadata()
//        for (key, value) in dict {
//            if let metadataValue = convertToMetadataValue(value) {
//                metadata[key] = metadataValue
//            }
//        }
//        return metadata
//    }
//
//    private func convertToMetadataValue(_ value: Any) -> MetadataValue? {
//        if value is NSNull {
//            return nil
//        } else if let value = value as? String {
//            return .string(value)
//        } else if let value = value as? Int {
//            return .int(value)
//        } else if let value = value as? Float {
//            return .float(value)
//        } else if let value = value as? [Any] {
//            return .array(value.compactMap { convertToMetadataValue($0) })
//        } else if let value = value as? [String: Any] {
//            return .dictionary(convertToMetadata(value))
//        }
//        return nil
//    }
//
//    private func convertMetadataToDictionary(_ metadata: Metadata) -> [String: Any] {
//        var dict = [String: Any]()
//        for (key, value) in metadata {
//            dict[key] = convertMetadataValueToAny(value)
//        }
//        return dict
//    }
//
//    private func convertMetadataValueToAny(_ value: MetadataValue) -> Any {
//        switch value {
//        case .string(let stringValue):
//            return stringValue
//        case .int(let intValue):
//            return intValue
//        case .float(let floatValue):
//            return floatValue
//        case .array(let arrayValue):
//            return arrayValue.map { convertMetadataValueToAny($0) }
//        case .dictionary(let dictValue):
//            return convertMetadataToDictionary(dictValue)
//        }
  //  }

    // Function to send events to React Native
    private func sendEvent(withName name: String, body: String) {
        guard let bridge = RCTBridge.current() else { return }
        bridge.eventDispatcher().sendAppEvent(withName: name, body: body)
    }
  
  
}
