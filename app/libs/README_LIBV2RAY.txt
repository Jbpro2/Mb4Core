Place your libv2ray.aar here.

- File name must be exactly: libv2ray.aar
- This project includes a conditional Gradle dependency:
    if (file('libs/libv2ray.aar').exists()) { implementation files('libs/libv2ray.aar') }

After copying the AAR, sync Gradle and rebuild.
