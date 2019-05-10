#ifdef DOUBLE_FP
    #ifdef AMD_FP
        #pragma OPENCL EXTENSION cl_amd_fp64 : enable
    #else
        #ifndef CL_VERSION_1_2
	        #pragma OPENCL EXTENSION cl_khr_fp64 : enable
        #endif
    #endif
    #define varfloat double
    #define varfloat3 double3
	#define varfloat2 double2
    #define _255 255.0
	#define _0 0.0
#else
    #define varfloat float
    #define varfloat3 float3
	#define varfloat2 float2
    #define _255 255.0f
	#define _0 0.0f
#endif

#define collidethresh 0.001
#define maxrayiterations 200

//DISTANCE ESTIMATORS https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
//OPENCL SPECIFICATION https://www.khronos.org/registry/OpenCL/specs/opencl-2.1.pdf

varfloat DE_Sphere(varfloat3 vec, varfloat3 center, varfloat radius);
varfloat DE_Torus(varfloat3 vec, varfloat3 center, varfloat2 t);
varfloat DE_Box(varfloat3 vec, varfloat3 center, varfloat3 b);
varfloat DE_Sponge(varfloat3 vec);
varfloat DE_Mandelbulb(varfloat3 vec, int* i);
//varfloat DE(varfloat3 vec);

varfloat DE_Sphere(varfloat3 vec, varfloat3 center, varfloat radius) { 
	return distance(vec, center) - radius;
}

varfloat DE_Torus(varfloat3 vec, varfloat3 center, varfloat2 t) {
	varfloat3 p = vec - center;
	return length((varfloat2)(length(p.xz)-t.x, p.y)) - t.y;
}

varfloat DE_Box(varfloat3 vec, varfloat3 center, varfloat3 b) {
	varfloat3 d = fabs(vec-center) - b;
	return length(fmax(d, _0)) + fmin(fmax(d.x,fmax(d.y,d.z)),_0);
}

#define DE_Iters 50
#define bailout 1.5
#define power 8

varfloat DE_Sponge(varfloat3 vec) {
	varfloat t;
	for(int n = 0; n < DE_Iters; n++){
		vec = fabs(vec);
		if(vec.x < vec.y) {
			t = vec.x;
			vec.x = vec.y;
			vec.y = t;
		}
		if(vec.y < vec.z) {
			t = vec.y;
			vec.y = vec.z;
			vec.z = t;
		}
		if(vec.x < vec.y) {
			t = vec.x;
			vec.x = vec.y;
			vec.y = t;
		}
		vec = 3.0f * vec - (varfloat3)(2.0f, 2.0f, 2.0f);
		if(vec.z < -1.0f) {
			vec.z += 2.0f;
		}
	}
	return (length(vec)-1.5f)*pow(3.0f, -(varfloat)DE_Iters);
}

varfloat DE_Mandelbulb(varfloat3 vec, int* iterations) {
	varfloat3 z = vec;
	varfloat dr = 1.0f;
	varfloat r = _0;
	int i;
	for (i = 0; i < DE_Iters; i++) {
		r = length(z);
		if (r > bailout) break;
		
		// convert to polar coordinates
		varfloat theta = acos(z.z/r);
		varfloat phi = atan(z.y/z.x);
		dr =  pow(r, power-1.0f)*power*dr + 1.0f;
		
		// scale and rotate the point
		varfloat zr = pow(r,power);
		theta = theta*power;
		phi = phi*power;
		
		// convert back to cartesian coordinates
		z = zr*(varfloat3)(sin(theta)*cos(phi), sin(phi)*sin(theta), cos(theta));
		z += vec;
	}
	if(i > iterations) {
		iterations = i;
	}
	return 0.5f*log(r)*r/dr;
}

/*varfloat DE(varfloat3 vec) {
	
}*/

kernel void raymarch(const int width, 
					 const int height,
					 __write_only image2d_t output,
					 const varfloat fov, 
					 const varfloat px, 
					 const varfloat py, 
					 const varfloat pz,
					 __constant float *m) {
	
	int2 pixelcoords = {get_global_id(0), get_global_id(1)};
	
	//varfloat y = yaw - fov/2.0 + fov*ix/width;
	//varfloat f = (fov * height) / width;
	//varfloat p = pitch - f/2.0 + f*iy/height;
	//varfloat3 direction = {sin(y)*cos(p), cos(y)*cos(p), sin(p)};
	
    varfloat3 position = {px, py, pz};
	
	varfloat2 p = {-width+2.0*pixelcoords.x, -height+2.0*pixelcoords.y};
	
	p /= (varfloat)height;
	
	varfloat3 temp = normalize((varfloat3)(p.x, p.y, 2.0));
	//012345678
	//ABCDEFGHI
	//P1   A D G   P1A+P2D+P3G
	//P2 * B E H = P1B+P2E+P3H
	//P3   C F I   P1C+P2F+P3I
	
	varfloat3 direction = {temp.x*m[0]+temp.y*m[3]+temp.z*m[6], 
						   temp.x*m[1]+temp.y*m[4]+temp.z*m[7],
						   temp.x*m[2]+temp.y*m[5]+temp.z*m[8]};
	
	//varfloat2 p = ((varfloat2)(width, height) + 2.0*(varfloat2)(pixelcoords))/(varfloat)(height);
    
	//direction = {0, 1, 0};
	
    int rayiterations = 0;
    varfloat currentDist = 1;
    
	int bulbiterations = 0;
	
    while(rayiterations < maxrayiterations && currentDist > collidethresh) {
    	currentDist = DE_Mandelbulb(position, &bulbiterations);
		position += direction * currentDist;
    	rayiterations++;
    }
	
	varfloat color = _255*(maxrayiterations-rayiterations)/maxrayiterations;
	//varfloat color = _255*(DE_Iters-bulbiterations)/(DE_Iters);
	write_imageui(output,  pixelcoords, (uint4)(color, color, color, 255));
}