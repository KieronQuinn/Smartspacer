# Target: Flight

Shows a boarding pass, seat number, gate **or** a textual or image hint.

### Feature ID

`SmartspaceTarget.FEATURE_FLIGHT` (`4`)

### Preview

Boarding pass UI:

![image](https://user-images.githubusercontent.com/3430869/142784887-b6333158-6cf1-4a62-8888-d70c1ddb4f29.png)

Generic UI:

![image](https://user-images.githubusercontent.com/3430869/142784902-9f16e27d-8ec4-4187-bcc2-dc462f7fe17c.png)


### Requirements

The base action should contain either:

| Key | Type | Notes |
| - | - | - |
| qrCodeBitmap | Bitmap (Parcelable) | Bitmap image, either of the actual QR code of the pass, or a dummy code |
| gate | String | Boarding gate |
| seat | String | Allocated seat | 

* This will show a boarding pass specific UI

\- or -

| Key | Type | Notes |
| - | - | - |
| cardPrompt | String | Short card/pass info | 
| cardPromptBitmap | Bitmap (Parcelable) | Bitmap image of a logo or similar |

* This will show a generic card UI (similar to loyalty cards)
