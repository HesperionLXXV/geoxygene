#version 330

/* Data structure sent to subshaders */
struct DataPainting {
	float screenWidth; 		// screen width in pixels 
	float screenHeight;		// screen height in pixels
	float mapScaleDiv1000;  // map scale divide by 1000 (e.g. 1:100000 maps, this value is 100)
	int brushWidth; 	    // brush texture width in pixels
	int brushHeight;        // brush texture height in pixels
	int brushStartWidth;    // start texture length in pixels for the brush
	int brushEndWidth;      // end texture length in pixels for the brush
	float brushScale;       // size in mm of one brush pixel
	float paperScale;       // scaling factor for paper
	float sharpness;        // brush-paper blending sharpness
	
	float paperDensity;     // paper height scale factor
	float brushDensity;     // brush height scale factor
	float strokePressure;   // stroke pressure
	vec4 position;          // current point position in world coordinates
	vec2 uv;                // UV coordinates texture (u in world coordinates, v between 0 and 1)
	vec4 color;             // point color
	float thickness;        // line thickness in world coordinates
	float uMax;             // maximum u coordinate in one polyline (in wolrd coordinates)
	vec2 tan;               // tangent vector at the given point (in world coordinates)
	float curvature;        // signed curvature estimation
	
};


// v is scaled from [0..1] to [0.5-width/2..0.5+width/2]
float vTextureScale( in float width, in float v ) {
    return (v - 0.5 + width/2.0)/width;
	float scaledV = 0.5 + (v - 0.5) / width;
	if ( scaledV < 0 ) return 0;
	if ( scaledV > 1 ) return 1;
	return scaledV;
}

float computeStrokeWidth( in float u, in float uMax ) {
	return 1;
}

float computeStrokePressure( in float u, in float uMax ) {
      return  u/uMax;
}


/************************************************************************************/
vec2 computeBrushTextureCoordinates( DataPainting fragmentData ) {
	float u_w = fragmentData.uv.x;
	float u_tex = 0;
	float strokeWidth = computeStrokeWidth( fragmentData.uv.x, fragmentData.uMax);
	float v_tex = vTextureScale( strokeWidth , fragmentData.uv.y );

	float brushStartLength_w = fragmentData.brushStartWidth * fragmentData.brushScale;
	float brushEndLength_w = fragmentData.brushEndWidth * fragmentData.brushScale;
	float brushMiddleLength_w = (fragmentData.brushWidth - fragmentData.brushStartWidth - fragmentData.brushEndWidth) * fragmentData.brushScale;
	
	float brush0_tex = fragmentData.brushStartWidth / float(fragmentData.brushWidth);
	float brush1_tex = 1f - fragmentData.brushEndWidth / float(fragmentData.brushWidth);
	if ( u_w <= brushStartLength_w ) {
		u_tex = (u_w / brushStartLength_w) * brush0_tex;
	} else if ( u_w >= fragmentData.uMax - brushEndLength_w ) {
		u_tex = ( u_w - ( fragmentData.uMax - brushEndLength_w ) ) * ( 1 - brush1_tex ) / brushEndLength_w + brush1_tex;
	} else {
		float polylineMiddleLength_w = fragmentData.uMax - (brushStartLength_w + brushEndLength_w);
		int nbTiles = max ( int( round( polylineMiddleLength_w / brushMiddleLength_w ) ), 1 );
		int nTile = int((u_w - brushStartLength_w )/(polylineMiddleLength_w / float(nbTiles)));
		float tileSize_w = polylineMiddleLength_w / float(nbTiles);
		u_tex = mod( u_w - brushStartLength_w, tileSize_w) / tileSize_w * ( brush1_tex - brush0_tex ) + brush0_tex; 
	}

	return vec2(u_tex, v_tex);
	
}

/************************************************************************************/
vec4 computeFragmentColor( in vec4 brushColor, in vec4 paperColor, in DataPainting fragmentData ) {     
	vec3 paperHeightField = paperColor.rgb;	
	float pressure = computeStrokePressure(fragmentData.uv.x, fragmentData.uMax) * fragmentData.strokePressure;
	float bh = 1-(1-brushColor.r) * pressure;
	float ph = (0.8 + (paperHeightField.x-0.5)*0.2);
	float penetration = clamp( ph - (1-bh), -1, 1);
	float f = smoothstep( 0.5 -.04, 0.5 +0.04,  penetration );
//	return vec4( vec3(penetration), 1 );
//	return vec4( computeBrushTextureCoordinates(fragmentData), 0, 1 );
	return vec4( fragmentData.color.rgb, ( 1.0 - f ) );
}
