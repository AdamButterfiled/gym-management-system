# BOOMGYM iOS Native App

This folder contains a native SwiftUI iOS app converted from the uni-app member mini-program template.

## Open

Open `BoomGymMemberApp.xcodeproj` in Xcode.

## Build

The project target is `BoomGymMemberApp`.

```sh
xcodebuild -project ios/BoomGymMemberApp.xcodeproj -scheme BoomGymMemberApp -configuration Debug -sdk iphonesimulator26.4 -arch arm64 build
```

The current local machine needs an installed iOS Simulator runtime before Xcode can finish asset compilation and launch the app.

## Scope

- Native SwiftUI, no WebView.
- Bottom tabs: 首页, 预约, 我的.
- Native pages for store, QR code, cards, coaches, booking detail, booking confirm, activity detail, profile, and login placeholder.
- Data is local mock data ported from the original template.
