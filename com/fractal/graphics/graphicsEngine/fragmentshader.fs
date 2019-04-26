#version 150
                 
uniform isampler2D raymarch; 
                
in vec2 texCoord; 
                 
out vec4 fragColor; 
                 
void main(void) { 
	fragColor = texture(raymarch, texCoord) / 255.0; 
}