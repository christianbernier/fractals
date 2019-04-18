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
    #define _255 255.0
#else
    #define varfloat float
    #define varfloat3 float3
    #define _255 255.0f
#endif

//DISTANCE ESTIMATORS https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
//OPENCL SPECIFICATION https://www.khronos.org/registry/OpenCL/specs/opencl-2.1.pdf

varfloat DE_Sphere(varfloat3 vec, varfloat3 center, varfloat radius) { 
	return distance(vec, center) - radius;
}

varfloat DE(varfloat3 vec) {
	varfloat dist;
    
    dist = DE_Sphere(vec, (varfloat3)(0, 0, 0), 1);
    //dist = min(dist, DE_Sphere(vec, (varfloat3){0, 0, 4}, 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3){0, 4, 0}, 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3){4, 0, 0}, 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3){4, 4, 0}, 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3){4, 0, 4}, 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3){0, 4, 4}, 1));
    //dist = min(dist, DE_Sphere(vec, (varfloat3){4, 4, 4}, 1));
    
    return dist;
}

kernel void mandelbrot(const int width, 
					   const int height,
					   __write_only image2d_t output,
					   const varfloat fov, 
					   const varfloat px, 
					   const varfloat py, 
					   const varfloat pz,
					   const varfloat pitch, 
					   const varfloat yaw/*,
					   const varfloat collidethresh, 
					   const int maxiterations,*/) {
					   
	//const varfloat fov = 3.14159/3;
	//const varfloat px = -10; 
	//const varfloat py = 0;
	//const varfloat pz = 0;
	//const varfloat pitch = 0;
	//const varfloat yaw = 0;
	const varfloat collidethresh = 0.1;
	const int maxiterations = 200;
	
	unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);
    
	varfloat y = yaw - fov/2.0 + fov*ix/width;
	//varfloat f = fov * height / width;
	//varfloat p = pitch - f/2.0 + f*iy/height;
	varfloat p = pitch - fov/2.0 + fov*iy/height;
	
    varfloat3 position = {px, py, pz};
    
	varfloat3 direction = {cos(y)*cos(p), -sin(y), -cos(y)*sin(p)};
	
	/*	(1, 0, 0)
	 *	(cos(yaw), -sin(yaw), 0)
	 *  (cos(yaw)cos(pitch), -sin(yaw), -cos(yaw)sin(pitch))
	 */
	//HAS TO BE INTRINSIC ROTATION: PITCH THEN YAW CURRENTLY YAW THEN PITCH
    
    int iterations = 0;
    varfloat currentDist = 0;
    
    while(iterations < maxiterations) { // && currentDist > collidethresh
    	currentDist = DE(position);
		position += direction*currentDist;
    	iterations++;
    }
	
	/*if(iterations >= maxiterations) {
		write_imageui(output, (int2)(ix, iy), (uint4)128);
	} else {
		varfloat color = _255*(maxiterations-iterations)/maxiterations;
		write_imageui(output, (int2)(ix, iy), (uint4)(color, 0, 0, 255));
	}*/
	
	write_imageui(output, (int2)(ix, iy), (uint4)((currentDist*10), 0, 0, 255));
}