#ifdef DOUBLE_FP
    #ifdef AMD_FP
        #pragma OPENCL EXTENSION cl_amd_fp64 : enable
    #else
        #ifndef CL_VERSION_1_2
	        #pragma OPENCL EXTENSION cl_khr_fp64 : enable
        #endif
    #endif
    #define varfloat double
	#define varfloat2 double2
    #define varfloat3 double3
	#define varfloat4 double4
    #define _255 255.0
	#define _0 0.0
	#define _1 1.0
	#define fov 1.0471975512 //PI/3
	#define collidethresh 0.000001
#else
    #define varfloat float
	#define varfloat2 float2
    #define varfloat3 float3
	#define varfloat4 float4
    #define _255 255.0f
	#define _0 0.0f
	#define _1 1.0f
	#define fov 1.0471975512f //PI/3 
	#define collidethresh 0.000001f
#endif

//DISTANCE ESTIMATORS https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
//OPENCL SPECIFICATION https://www.khronos.org/registry/OpenCL/specs/opencl-2.1.pdf

varfloat DE_Sphere(varfloat3 vec, varfloat3 center, varfloat radius);
varfloat DE_Torus(varfloat3 vec, varfloat3 center, varfloat2 t);
varfloat DE_Box(varfloat3 vec, varfloat3 center, varfloat3 b);
varfloat DE_Sponge(varfloat3 vec, int DE_Iters);
varfloat DE_Mandelbulb(varfloat3 vec, int DE_Iters);
void sphereFold(varfloat3 *vec, varfloat *dz);
void boxFold(varfloat3 *vec);
varfloat DE_Mandelbox(varfloat3 vec, int DE_Iters);
varfloat DE_Mandelbox_T(varfloat3 vec, int DE_Iters, varfloat t);
varfloat3 Hue(varfloat hue);
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

#define bailout 1.5f
#define power 8.0f

varfloat DE_Sponge(varfloat3 vec, int DE_Iters) {
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

varfloat DE_Mandelbulb(varfloat3 vec, int DE_Iters) {
	varfloat dist;
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
	dist = 0.5f*log(r)*r/dr;
	return dist;
}

#define scale -1.5f
#define fixedRadius 1.0f
#define fixedRadius2 1.0f
#define minRadius 0.5f
#define minRadius2 0.25f

void sphereFold(varfloat3 *vec, varfloat *dz) {
	varfloat r2 = dot(*vec, *vec); //x*x + y*y + z*z
	if (r2<minRadius2) { 
		// linear inner scaling
		varfloat temp = fixedRadius2 / minRadius2;
		*vec *= temp;
		*dz *= temp;
	} else if (r2<fixedRadius2) { 
		// this is the actual sphere inversion
		varfloat temp = fixedRadius2 / r2;
		*vec *= temp;
		*dz *= temp;
	}
}

#define foldingLimit 1.0f

void boxFold(varfloat3 *vec) {
	*vec = clamp(*vec, -foldingLimit, foldingLimit) * 2.0f - *vec;
}

varfloat DE_Mandelbox(varfloat3 vec, int DE_Iters) {
	varfloat3 offset = vec;
	varfloat dr = _1;
	for(int n = 0; n < DE_Iters; n++) {
		boxFold(&vec);       // Reflect
		sphereFold(&vec, &dr);    // Sphere Inversion
 		vec = scale*vec + offset;  // Scale & Translate
        dr = dr*fabs(scale)+ _1;
    }
	return length(vec) / fabs(dr);
}

varfloat DE_Mandelbox_t(varfloat3 vec, int DE_Iters, varfloat t) {
	t += 0.5;
	t *= scale;
	varfloat3 offset = vec;
	varfloat dr = t;//1.0;
	for(int n = 0; n < DE_Iters; n++) {
		boxFold(&vec);       // Reflect
		sphereFold(&vec, &dr);    // Sphere Inversion
 		vec = t*vec + offset;  // Scale & Translate
        dr = dr*fabs(t)+1.0f;
	}
	return length(vec) / fabs(dr);
}

/*varfloat DE(varfloat3 vec) {
	
}*/

varfloat3 Hue(varfloat hue) { //Hue is from 0 to 1
	hue *= 6.0f;
	varfloat x = _1-fabs(fmod(hue, 2.0f) - _1);
	switch((int)hue) {
		case 0:
			return (varfloat3)(_1, x, _0);
			break;
		case 1:
			return (varfloat3)(x, _1, _0);
			break;
		case 2:
			return (varfloat3)(_0, _1, x);
			break;
		case 3:
			return (varfloat3)(_0, x, _1);
			break;
		case 4:
			return (varfloat3)(x, _0, _1);
			break;
	}
	return (varfloat3)(_1, _0, x);
}

kernel void raymarch(const int width, 
					 const int height,
					 __write_only image2d_t output,
					 const varfloat px, 
					 const varfloat py, 
					 const varfloat pz,
					 __constant float *m,
					 const int maxrayiterations,
					 const int DE_Iters,
					 const varfloat t) {

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
	varfloat hue;
	
    while(rayiterations < maxrayiterations && currentDist > collidethresh) {
    	currentDist = DE_Mandelbox_t(position, DE_Iters, t);
		position += direction * currentDist;
    	rayiterations++;
    }
	
	varfloat color = _255*(maxrayiterations-rayiterations)/maxrayiterations;
	varfloat3 colorvec = Hue(fmod(length(position), 1.0))*color;
	//varfloat color = _255*(DE_Iters-bulbiterations)/(DE_Iters);
	write_imageui(output,  pixelcoords, (uint4)(colorvec.x, colorvec.y, colorvec.z, 255));
}
