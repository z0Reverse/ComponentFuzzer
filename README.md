This application automatically detects exported components in Android apps and constructs intents specifically to check if there are crash risks, greatly improving testing efficiency.

Instructions for Use:
- Start adb log monitoring:
  ```bash
  logcat -v brief | grep -E "Auto Fuzzer|CRASH DETECTED"
  ```
- Enter the application package name in the app interface and click on autoFuzzer to execute.
