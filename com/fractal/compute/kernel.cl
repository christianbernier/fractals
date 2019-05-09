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
#define maxiterations 200

//DISTANCE ESTIMATORS https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
//OPENCL SPECIFICATION https://www.khronos.org/registry/OpenCL/specs/opencl-2.1.pdf

varfloat DE_Sphere(varfloat3 vec, varfloat3 center, varfloat radius);
varfloat DE_Torus(varfloat3 vec, varfloat3 center, varfloat2 t);
varfloat DE_Box(varfloat3 vec, varfloat3 center, varfloat3 b);
varfloat DE(varfloat3 vec);

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

varfloat DE(varfloat3 vec) {
	varfloat dist;
	
	vec.x = fmod(fabs(vec.x), 1);
	vec.y = fmod(fabs(vec.y), 1);
	vec.z = fmod(fabs(vec.z), 1);
	
    //dist = DE_Box(vec, (varfloat3)(0.5, 0.5, 0.5), (varfloat3)(0.2, 0.2, 0.2));
    dist = DE_Torus(vec, (varfloat3)(0.5, 0.5, 0.5), (varfloat2)(0.20, 0.05));
	//dist = min(dist, DE_Torus(vec, (varfloat3)(1, 0, 0), (varfloat2)(0.20, 0.05)));
	//dist = min(dist, DE_Torus(vec, (varfloat3)(-1, 0, 0), (varfloat2)(0.20, 0.05)));
	
	//dist = DE_Sphere(vec, (varfloat3)(2, 2, 2), 1);
	//dist = min(dist, DE_Sphere(vec, (varfloat3)(0, 0, 4), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(0, 0, -4), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(0, 4, 0), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(4, 0, 0), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(4, 4, 0), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(4, 0, 4), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(0, 4, 4), 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3)(4, 4, 4), 1));
    
    return dist;
}

kernel void raymarch(const int width, 
					 const int height,
					 __write_only image2d_t output,
					 const varfloat fov, 
					 const varfloat px, 
					 const varfloat py, 
					 const varfloat pz,
					 __constant float *m/*,
					 const varfloat collidethresh, 
					 const int maxiterations,*/) {
	
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
	
    int iterations = 0;
    varfloat currentDist = 1;
    
    while(iterations < maxiterations && currentDist > collidethresh) {
    	currentDist = DE(position);
		position += direction * currentDist;
    	iterations++;
    }
	
	varfloat color = _255*(maxiterations-iterations)/maxiterations;
	write_imageui(output,  pixelcoords, (uint4)(color, color, color, 255));
}