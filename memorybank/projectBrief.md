# Project Brief: HUNTERpixel

## Overview
HUNTERpixel is a Python API supported Android app that enables children to explore their physical environment. Users build a digital collection by gathering objects guided by the SAM-r robot.

## Core Mechanics
* Objects framed within a fixed square are captured via a button trigger.
* Images are analyzed through the Python API. If validated a background removed sticker is returned.
* Successful captures are stored in the Room Database along with score and date data.

## Technical Foundation
* Platform: Android Kotlin.
* AI Architecture: Server side API execution of SAM 1 and MobileNet.
* Data Management: In app Room Database implementation.
* Education: Multimodal audio and text guidance via the SAM-r character.