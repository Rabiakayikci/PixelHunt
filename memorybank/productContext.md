# Product Context: HUNTERpixel

## Project Purpose
To encourage children to explore the physical world by using the screen as a magnifying glass rather than being confined within it.

## Heart of the Game: SAM-r Mission System
1. Mission Assignment: SAM-r assigns the child a target by randomly selecting a safe object from the local list.
2. Clue System: SAM-r provides the object features both in a speech bubble and vocally via TTS.
3. Targeting and Capture: The child focuses the object within the square frame on the screen and presses the capture button.
4. Smart Validation: The image is sent to the Python API. If correct, the object is segmented via SAM 1 on the server, a sticker is created and added to the album. If incorrect, SAM-r gives another educational clue to encourage a retry.