package com.rakesh1308.hnreader.platform

import platform.UIKit.UIDevice

actual fun platformName(): String =
    UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion
