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
	#define collidethresh 0.0001
	#define maxraylength 5
	#define PI 3.14159
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
	#define maxraylength 5
	#define PI 3.14159f
#endif

#define DEFUNCTION DE_Mandelbox_c(position, DE_Iters, &c)

//DISTANCE ESTIMATORS https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
//OPENCL SPECIFICATION https://www.khronos.org/registry/OpenCL/specs/opencl-2.1.pdf

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
#define power 12.0f

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

varfloat DE_Mandelbulb_t(varfloat3 vec, int DE_Iters, varfloat t) {
	varfloat dist;
	varfloat3 z = vec;
	varfloat dr = 1.0f;
	varfloat r = _0;
	t *= power;
	int i;
	for (i = 0; i < DE_Iters; i++) {
		r = length(z);
		if (r > bailout) break;
		
		// convert to polar coordinates
		varfloat theta = acos(z.z/r);
		varfloat phi = atan(z.y/z.x);
		dr =  pow(r, t-1.0f)*t*dr + 1.0f;
		
		// scale and rotate the point
		varfloat zr = pow(r,t);
		theta = theta*t;
		phi = phi*t;
		
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

varfloat DE_Mandelbox_c(varfloat3 vec, int DE_Iters, varfloat *c) {
	varfloat3 offset = vec;
	varfloat dr = _1;
	varfloat len;
	for(int n = 0; n < DE_Iters; n++) {
		boxFold(&vec);       // Reflect
		sphereFold(&vec, &dr);    // Sphere Inversion
 		vec = scale*vec + offset;  // Scale & Translate
        dr = dr*fabs(scale)+ _1;
        len = length(vec);
        if(len < *c) {
        	*c = len;
        }
    }
	return len / fabs(dr);
}

varfloat DE_Mandelbox_t(varfloat3 vec, int DE_Iters, varfloat t) {
	t += 0.5f;
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

varfloat2 fold(varfloat2 vec, varfloat ang){
    varfloat2 n = (varfloat2)(cos(-ang), sin(-ang));
    vec -= 2.0f * min(_0, dot(vec, n)) * n;
    return vec;
}

varfloat3 tri_fold(varfloat3 vec, varfloat t) {
    vec.xy = fold(vec.xy, PI/3.0f - cos(t)/10.0f);
    vec.xy = fold(vec.xy, -PI/3.0f);
    vec.yz = fold(vec.yz, -PI/6.0f + sin(t)/2.0f);
    vec.yz = fold(vec.yz, PI/6.0f);
    return vec;
}

varfloat DE_Koch_t(varfloat3 vec, varfloat t){
    vec *= 0.75f;
    vec.x += 1.5f;
    for(int i = 0; i < 8; i++){
        vec *= 2.0f;
        vec.x -= 2.6f;
        vec = tri_fold(vec, t);
    }
    return length( vec*0.004f ) - 0.01f;
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

varfloat3 reflect(varfloat3 incident, varfloat3 point) {
	return incident;
}

kernel void raymarch(const int width, 				//0
					 const int height,				//1
					 __write_only image2d_t output, //2
					 const varfloat px, 			//3	
					 const varfloat py, 			//4
					 const varfloat pz,				//5
					 __constant float *mat,			//6
					 const int maxrayiterations,	//7
					 const int DE_Iters,			//8
					 const varfloat t,				//9
					 const int AA) {				//10

	int2 pixelcoords = {get_global_id(0), get_global_id(1)};
	
	//varfloat y = yaw - fov/2.0 + fov*ix/width;
	//varfloat f = (fov * height) / width;
	//varfloat p = pitch - f/2.0 + f*iy/height;
	//varfloat3 direction = {sin(y)*cos(p), cos(y)*cos(p), sin(p)};
	
	varfloat3 position = {px, py, pz};
	
	varfloat3 colorvec = {0, 0, 0};
	
	varfloat2 p;
	varfloat3 temp;
	varfloat3 direction;
	int rayiterations;
	varfloat currentDist;
	varfloat raylength;
	bool collided;
	
	varfloat z = 0;
	
	varfloat c;
	
	for(int m = 0; m < AA; m++) {
		p.x = (-width+2.0*(pixelcoords.x + (varfloat)m/(varfloat)AA - 0.5))/(varfloat)height;
		for(int n = 0; n < AA; n++) {
			p.y = (-height+2.0*(pixelcoords.y + (varfloat)n/(varfloat)AA - 0.5))/(varfloat)height;
			
			temp = normalize((varfloat3)(p.x, p.y, 2.0));
			
			position = (varfloat3)(px, py, pz);
			
			//012345678
			//ABCDEFGHI
			//P1   A D G   P1A+P2D+P3G
			//P2 * B E H = P1B+P2E+P3H
			//P3   C F I   P1C+P2F+P3I
			
			direction.x = temp.x * mat[0] + temp.y * mat[3] + temp.z * mat[6]; 
			direction.y = temp.x * mat[1] + temp.y * mat[4] + temp.z * mat[7];
			direction.z = temp.x * mat[2] + temp.y * mat[5] + temp.z * mat[8];
			
			//varfloat2 p = ((varfloat2)(width, height) + 2.0*(varfloat2)(pixelcoords))/(varfloat)(height);
			
			rayiterations = 0;
			currentDist = 1;
			raylength = 0;
			collided = true;
			c = 10.0f;
			
			while(currentDist > collidethresh) {
				currentDist = DEFUNCTION;
				position += direction * currentDist;
				raylength += currentDist;
				rayiterations++;
				if(raylength >= maxraylength || rayiterations >= maxrayiterations) {
					collided = false;
					break;
				}
			}
			
			if(collided) {
				colorvec += Hue(c)*(maxrayiterations-rayiterations)/maxrayiterations; //fmod(length(position), 1.0)
			} else {
				colorvec += (varfloat3)(0, 0, 0);
			}
		}
	}
	
	if(AA > 1) {
		colorvec /= (varfloat)(AA*AA);
	}
	
	//colorvec *= _255;
	
	write_imageui(output, pixelcoords, (uint4)(255*colorvec.x, 255*colorvec.y, 255*colorvec.z, 255));
}
