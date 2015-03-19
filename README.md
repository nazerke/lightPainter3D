# lightPainter3D

- Uses rajawali library v0.9. Download here tag v0.9 https://github.com/Rajawali/Rajawali/tags
- The library version has a bug. Add buffer.position(0) in Geometry3D.createBuffer() before binding.      
    buffer.position(0);
		GLES20.glBindBuffer(target, handle);
		...
