---
description: Generate Android app icons from a source image
---
1. Resize Akemi.png to 48x48 for mdpi
// turbo
2. cp Akemi.png app/src/main/res/mipmap-mdpi/ic_launcher.png
// turbo
3. cp Akemi.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
// turbo
4. sips -z 48 48 app/src/main/res/mipmap-mdpi/ic_launcher.png
// turbo
5. sips -z 48 48 app/src/main/res/mipmap-mdpi/ic_launcher_round.png

6. Resize Akemi.png to 72x72 for hdpi
// turbo
7. cp Akemi.png app/src/main/res/mipmap-hdpi/ic_launcher.png
// turbo
8. cp Akemi.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
// turbo
9. sips -z 72 72 app/src/main/res/mipmap-hdpi/ic_launcher.png
// turbo
10. sips -z 72 72 app/src/main/res/mipmap-hdpi/ic_launcher_round.png

11. Resize Akemi.png to 96x96 for xhdpi
// turbo
12. cp Akemi.png app/src/main/res/mipmap-xhdpi/ic_launcher.png
// turbo
13. cp Akemi.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
// turbo
14. sips -z 96 96 app/src/main/res/mipmap-xhdpi/ic_launcher.png
// turbo
15. sips -z 96 96 app/src/main/res/mipmap-xhdpi/ic_launcher_round.png

16. Resize Akemi.png to 144x144 for xxhdpi
// turbo
17. cp Akemi.png app/src/main/res/mipmap-xxhdpi/ic_launcher.png
// turbo
18. cp Akemi.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
// turbo
19. sips -z 144 144 app/src/main/res/mipmap-xxhdpi/ic_launcher.png
// turbo
20. sips -z 144 144 app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png

21. Resize Akemi.png to 192x192 for xxxhdpi
// turbo
22. cp Akemi.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
// turbo
23. cp Akemi.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
// turbo
24. sips -z 192 192 app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
// turbo
25. sips -z 192 192 app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
