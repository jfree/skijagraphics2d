SkijaGraphics2D
===============

Version 1.0.1, 27 July 2021

Overview
--------
**SkijaGraphics2D** is a free implementation of Java2D's `Graphics2D` API that targets Skia via the [Skija](https://github.com/JetBrains/skija) bindings.

The following sample is created using [Orson Charts](https://github.com/jfree/orson-charts):

![SkijaGraphics2D sample](sample.png)

Include
-------
To include `SkijaGraphics2D` in your own project, add the following Maven dependency:

        <dependency>
            <groupId>org.jfree</groupId>
            <artifactId>org.jfree.skijagraphics2d</artifactId>
            <version>1.0.1</version>
        </dependency>

Build
-----
You can build `SkijaGraphics2D` from sources using Maven:

    mvn clean install

License
-------

`SkijaGraphics2D` is licensed under a BSD-style license:

```
Copyright (c) 2021, Object Refinery Limited.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors 
may be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

History
-------

##### not-yet-released : Version 1.0.2
- fix `setClip()` to restore original clip before applying new user clip
- set `PathFillMode` when filling paths
- add `MIN_LINE_WIDTH` to apply for `BasicStroke(0f)`
- fix exception when `Rectangle2D` has negative `width` or `height`

##### 27-Jul-2021 : Version 1.0.1
- added specialisation in `fill(Shape)` for `Rectangle2D` instances
- added caching of `Typeface` instances
- fix paint setting for font rendering
- added support for `AlphaComposite`
- apply clipping (still work-in-progress)
- fix for cyclic gradient paint
- updated `Skija` to version 0.92.18
- added logging via `SLF4J` and `Log4J2`

##### 21-Jul-2021 : Version 1.0.0
- initial public release.