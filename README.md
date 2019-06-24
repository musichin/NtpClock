# NTP Clock [ ![Download](https://api.bintray.com/packages/musichin/maven/NtpClock/images/download.svg)](https://bintray.com/musichin/maven/NtpClock/_latestVersion) [![Kotlin](https://img.shields.io/badge/Kotlin-1.3.40-blue.svg)](http://kotlinlang.org) [![Build Status](https://travis-ci.org/musichin/NtpClock.svg?branch=master)](https://travis-ci.org/musichin/NtpClock)

## Usage
```kotlin
NtpClock.sync().onSuccess { stamp ->
    println(stamp.millis()) // milliseconds from the epoch of 1970-01-01T00:00:00Z.
}
```

## Binaries
```groovy
repositories {
    maven { url 'https://dl.bintray.com/musichin/maven' }
}

dependencies {
    implementation 'de.musichin.ntpclock:ntpclock:x.y.z'
}
```

## Contributing
Contributors are more than welcome. Just create a PR with a description of your changes.

## Issues or feature requests?
File github issues for anything that is unexpectedly broken.

## License

    MIT License

    Copyright (c) 2019 Anton Musichin

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
