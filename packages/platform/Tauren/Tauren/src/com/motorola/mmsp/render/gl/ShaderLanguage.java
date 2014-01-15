package com.motorola.mmsp.render.gl;

public final class ShaderLanguage {
	
	public static final String VS = 
			  "//precision mediump float; \n"
			+ "uniform mat4 uMVPMatrix; \n"
			+ "uniform vec2 uv2ScaleFactor; \n"
			+ "uniform mat4 uProjMatrix; \n"
			+ "uniform vec4 uData[120]; \n"
			+ "attribute vec4 aPosition; \n"
			+ "attribute vec2 aTextureCoord; \n"
			+ "attribute float aIndex; \n"
			+ "varying vec2 vTextureCoord; \n"
			+ "varying float vAlpha; \n"

			+ "void main()  \n"
			+ "{"
			+ "	vec4 pos = aPosition; \n"
			+ "	mat4 matrix; \n"
			+ "	int c = int(aIndex); \n"
			+ "	c *= 4; \n"
	
			+ "	matrix[0][0] = uData[c].x; \n"
			+ "	matrix[1][0] = uData[c].y; \n"
			+ "	matrix[3][0] = uData[c].z; \n"
			+ "	matrix[0][1] = uData[c].w; \n"
			+ "	matrix[1][1] = uData[c + 1].x; \n"
			+ "	matrix[3][1] = uData[c + 1].y; \n"
			+ "	matrix[0][3] = uData[c + 1].z; \n"
			+ "	matrix[1][3] = uData[c + 1].w; \n"
			+ "	matrix[3][3] = uData[c + 2].x; \n"
			+ "	pos.x *= uData[c + 2].y; \n"
			+ "	pos.y *= uData[c + 2].z; \n"
			+ "	vAlpha = uData[c + 2].w; \n"
	
			+ "	gl_Position = uProjMatrix * matrix * pos; \n"
	
			+ "	vec2 textureCoord;"
			+ "	textureCoord.x = aTextureCoord.x * uData[c + 3].z; \n"
			+ "	textureCoord.y = aTextureCoord.y * uData[c + 3].w; \n"
			+ "	textureCoord.x += uData[c + 3].x; \n"
			+ "	textureCoord.y += uData[c + 3].y; \n"
			+ "	vTextureCoord = textureCoord; \n"
			+ "}";
	
	public static final String PS = 
			  "precision mediump float; \n"
			+ "varying vec2 vTextureCoord; \n"
			+ "uniform sampler2D sTexture; \n"
			+ "varying float vAlpha; \n"
			+ "void main() \n"
			+ "{ \n"
			+ "	vec4 color = texture2D(sTexture, vTextureCoord); \n"
			+ "	color.xyz /= color.w; \n"
			+ "	color.w *= vAlpha; \n"
			+ "	gl_FragColor.rgb = color.xyz; \n"
			+ "	gl_FragColor.a = color.w; \n"
			+ "}";
	
}
