//
//  Event.swift
//  Pods
//
//  Created by Anurag on 27/08/25.
//

import React

public class Event: RCTEventEmitter {
  func emit(name: String, body: String) {
    self.sendEvent(withName: name, body: body)
  }
}
