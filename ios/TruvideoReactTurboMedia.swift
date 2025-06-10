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
          
          let mainResponse: [String: Any] = [
            "id": request.id, // Generate a unique ID for the event
            "filePath": request.filePath,
            "fileType": request.fileType,
            "durationMilliseconds": request.durationMilliseconds ?? "",
            "remoteId" : request.remoteId ?? "",
            "remoteURL" : request.remoteURL ?? "",
            "transcriptionURL" : request.transcriptionURL ?? "",
            "transcriptionLength" : request.transcriptionLenght ?? "",
            "status" : request.status,
            "progress" : request.uploadProgress
          ]
          
          resolve(mainResponse)
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
                    "transcriptionURL": transcriptionURL,
                    "transcriptionLength": transcriptionLength
                ]
                
                // resolve
                resolve(["status": mainResponse])
                self.sendEvent(withName: "onComplete", body: mainResponse)
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
                self.sendEvent(withName: "onProgress", body: mainResponse)
            })
        
        // Store the progress handler in the dispose bag to avoid premature deallocation
        progress.store(in: &disposeBag)
        
        try request.upload()
    }
  
  @objc public func getFileUploadRequestById(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let result =  try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    print(result ?? "")
    //TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
  }

  @objc public func cancel(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.cancel()
  }
  
  @objc public func delete(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.delete()
  }
  
  @objc public func pause(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.pause()
  }
  
  @objc public func resume(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
    try? request?.resume()
  }
  
  @objc public func search(tag: String,type : String,page : String,pageSize : String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
    
    let tagDict = try? convertToDictionary(from: tag)
    var tagBuild = TruvideoSdkMediaTags.builder()
    for (key, value) in tagDict! {
      tagBuild.set(key, "\(value)")
    }
    Task{
      let request = try? await TruvideoSdkMedia.search(type: nil, tags: tagBuild.build(), pageNumber: Int(page) ?? 0, size: Int(pageSize) ?? 0)
    }
    //try? request?.resume()
  }
  
  
  @objc public func upload(id: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock){
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
            let mainResponse: [String: Any] = [
                "id": id ?? "", // Generate a unique ID for the event
                "createdDate" : uploadedResult.createdDate,
                "remoteId" : uploadedResult.remoteId,
                "uploadedFileURL": uploadedFileURL.absoluteString,
                "metaData": metadataDict.dictionary,
                "tags": tags.dictionary,
                "transcriptionURL": transcriptionURL ?? "",
                "transcriptionLength": transcriptionLength,
                "fileType" : uploadedResult.type.rawValue
            ]
            
            // resolve
            resolve(["status": mainResponse])
            self.sendEvent(withName: "onComplete", body: mainResponse)
        })
    
    // Store the completion handler in the dispose bag to avoid premature deallocation
    completeCancellable?.store(in: &disposeBag)
    
    // Progress of request
    let progress = request?.progressHandler
        .receive(on: DispatchQueue.main)
        .sink(receiveValue: { progress in
            let mainResponse: [String: Any] = [
                "id": UUID().uuidString, // Generate a unique ID for the event
                "progress": String(format: " %.2f %", progress.percentage * 100)
            ]
            self.sendEvent(withName: "onProgress", body: mainResponse)
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
    private func sendEvent(withName name: String, body: [String: Any]) {
        guard let bridge = RCTBridge.current() else { return }
        bridge.eventDispatcher().sendAppEvent(withName: name, body: body)
    }
}
