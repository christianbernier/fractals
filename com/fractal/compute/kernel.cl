#ifdef DOUBLE_FP
    #ifdef AMD_FP
        #pragma OPENCL EXTENSION cl_amd_fp64 : enable
    #else
        #ifndef CL_VERSION_1_2
	        #pragma OPENCL EXTENSION cl_khr_fp64 : enable
        #endif
    #endif
    #define varfloat double
    #define _255 255.0
#else
    #define varfloat float
    #define _255 255.0f
#endif

#ifdef USE_TEXTURE
    #define OUTPUT_TYPE __write_only image2d_t
#else
    #define OUTPUT_TYPE global uint *
#endif

static inline double distance(varfloat4 a, varfloat4 b) {
	return 
}

//DISTANCE ESTIMATORS https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
//OPENCL SPECIFICATION https://www.khronos.org/registry/OpenCL/specs/opencl-1.2.pdf

double DE(varfloat4 vec) { //Two spheres since one sphere is uniform no matter what angle we look at it
	
}

kernel void raymarch(const int width,       const int height,
					 const varfloat x0, 	const varfloat y0,
        			 const varfloat rangeX,	const varfloat rangeY) {
	unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);

    varfloat r = x0 + ix * rangeX / width;
    varfloat i = y0 + iy * rangeY / height;
}