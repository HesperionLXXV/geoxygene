#version 330
// @massea  
// Raster Fragment Shader with style (Colormap and Opacity)
// Specific : Tide animation (maybe too specific to be here)

// Const
const float PI = 3.14159265359;

// Uniform
// Image buffer
uniform sampler2D bufferImage;

// Style
// Opacity
uniform float globalOpacity = 1.0;
uniform float objectOpacity = 1.0;

// ColorMap
uniform int typeColormap = 0;
uniform int nbPointsColormap = 0;
uniform sampler2D bufferColormap;

// Animation
uniform float time = 0.0;
uniform int animate = 0;

// Tide information
uniform float waterHeightMean = 0.0;
uniform float tideRange = 0.0;
uniform float timeAcceleration = 20.0;
uniform float tideCycleLength = 43200.0;
uniform float tidePhase = 0.0;

// Vertex data in
in VertexData {
    vec4 color;
    vec2 textureUV;
} fragmentIn;

// Fragment out
out vec4 outColor;

// Interpolation function from Colormap entries
vec4 interpolateColor(vec4 pixel) 
{
    int prev = -1;
    int next = -1;

    vec4 pixel_value = vec4(pixel.x,pixel.x,pixel.x,pixel.x);
    for(int i=0; i<nbPointsColormap-1; i++)
    {
        vec4 value_i = texelFetch(bufferColormap,ivec2(i,1),0);
        vec4 value_i_1 = texelFetch(bufferColormap,ivec2(i+1,1),0);
        
        if ((pixel_value.x >= value_i.x)&&(pixel_value.x<=value_i_1.x )) 
        {
            prev = i;
            next = i+1;
            
            vec4 color_prev = texelFetch(bufferColormap,ivec2(prev,0),0);
            vec4 color_next = texelFetch(bufferColormap,ivec2(next,0),0);
            vec4 value_prev = texelFetch(bufferColormap,ivec2(prev,1),0);
            vec4 value_next = texelFetch(bufferColormap,ivec2(next,1),0);

            return vec4( (color_prev + (pixel_value-value_prev)*(color_next - color_prev)/(value_next-value_prev)) / vec4(255.0,255.0,255.0,255.0));
        }
    }
    if(pixel.x<texelFetch(bufferColormap,ivec2(0,1),0).x)
    {
        return texelFetch(bufferColormap,ivec2(0,0),0) / vec4(255.0,255.0,255.0,255.0);
    }
    else 
    {
        return vec4(0.0,0.0,0.0,0.0);
    }
}

// Categorize function from Colormap entries
vec4 categorizeColor(vec4 pixel) 
{
    vec4 pixel_value = vec4(pixel.x,pixel.x,pixel.x,pixel.x);
    
    for(int i=0; i<nbPointsColormap; i++)
    {        
        if (pixel_value.x == texelFetch(bufferColormap,ivec2(i,1),0).x)
        {
            return (texelFetch(bufferColormap,ivec2(i,0),0)  / vec4(255.0,255.0,255.0,255.0));    
        }
    }
    return vec4(0.0,0.0,0.0,0.0);
}

// Intervals function from Colormap entries
vec4 intervalsColor(vec4 pixel) 
{
    vec4 pixel_value = vec4(pixel.x,pixel.x,pixel.x,pixel.x);
    
    if(pixel_value.x<texelFetch(bufferColormap,ivec2(0,1),0).x) {
        return (texelFetch(bufferColormap,ivec2(0,0),0)  / vec4(255.0,255.0,255.0,255.0));
    }  
    
    for(int i=1; i<nbPointsColormap; i++)
    {        
        if (pixel_value.x <= texelFetch(bufferColormap,ivec2(i,1),0).x)
        {
            return (texelFetch(bufferColormap,ivec2(i,0),0)  / vec4(255.0,255.0,255.0,255.0));    
        }
    }
    return vec4(0.0,0.0,0.0,0.0);
}

// \o/ Main program \o/
void main(void) 
{
    // Image coordinates (screen)
    vec2 P = fragmentIn.textureUV;
    
    // The raster goes in the rectangle 
    vec4 pixel = texture(bufferImage,P);                  
    
    // Animation, tides and stuffs
    if(animate==1) {
        // TODO Get back tide information from files

        // New version with uniform
        float tideFrequency = 2.0 * PI / tideCycleLength * timeAcceleration;
        pixel.x = pixel.x - waterHeightMean + (tideRange * sin( tideFrequency * time / 1000.0 + tidePhase));            
    }
    
    // We apply the colormap 
    // 1: interpolation; 2: categorize; 3: intervals
    if (typeColormap == 1) {
        outColor = interpolateColor(pixel);
     } else if ( typeColormap == 2 ) {
        outColor = categorizeColor(pixel);
     } else if ( typeColormap == 3 ) {
        outColor = intervalsColor(pixel);
     }
     else {
        outColor = pixel;
     }
     
    // Opacity (multiplication)
    outColor.a = outColor.a*globalOpacity*objectOpacity;
}