# Trusted Invoice Intake

## Local Development Notes

### Apple Silicon (M1/M2/M3) — Tesseract / JNA
If you get a `UnsatisfiedLinkError` when running OCR locally on Apple Silicon,
Homebrew's native libs are outside JNA's default search path. Fix by adding
this JVM argument when running the app or tests:

    -Djna.library.path=/opt/homebrew/lib

In IntelliJ: Run → Edit Configurations → VM options → add the above.
This is a local dev issue only. The Docker image handles this automatically.
