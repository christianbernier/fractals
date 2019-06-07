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
	#define _0p5 0.5
	#define _0p25 0.25
	#define _1 1.0
	#define _1p5 1.5
	#define _2 2.0
	#define _3 3.0
	#define _6 6.0
	#define fov 1.0471975512 //PI/3
	#define collidethresh 0.000001
	#define PI 3.14159
#else
    #define varfloat float
	#define varfloat2 float2
    #define varfloat3 float3
	#define varfloat4 float4
    #define _255 255.0f
	#define _0 0.0f
	#define _0p5 0.5f
	#define _0p25 0.25f
	#define _1 1.0f
	#define _1p5 1.5f
	#define _2 2.0f
	#define _3 3.0f
	#define _6 6.0f
	#define fov 1.0471975512f //PI/3 
	#define collidethresh 0.000001f
	#define PI 3.14159f
#endif

#define maxraylength 5
#define maxcollisions 1

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

#define bailout _1p5
#define power 8

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
		vec = _3 * vec - (varfloat3)(_2, _2, _2);
		if(vec.z < -_1) {
			vec.z += _2;
		}
	}
	return (length(vec)-_1p5)*pow(_3, -(varfloat)DE_Iters);
}

varfloat DE_Mandelbulb(varfloat3 vec, int DE_Iters) {
	varfloat dist;
	varfloat3 z = vec;
	varfloat dr = _1;
	varfloat r = _0;
	int i;
	for (i = 0; i < DE_Iters; i++) {
		r = length(z);
		if (r > bailout) break;
		
		// convert to polar coordinates
		varfloat theta = acos(z.z/r);
		varfloat phi = atan(z.y/z.x);
		dr =  pow(r, power-_1)*power*dr + _1;
		
		// scale and rotate the point
		varfloat zr = pow(r,power);
		theta = theta*power;
		phi = phi*power;
		
		// convert back to cartesian coordinates
		z = zr*(varfloat3)(sin(theta)*cos(phi), sin(phi)*sin(theta), cos(theta));
		z += vec;
	}
	dist = _0p5*log(r)*r/dr;
	return dist;
}

varfloat DE_Mandelbulb_t(varfloat3 vec, int DE_Iters, varfloat t) {
	varfloat dist;
	varfloat3 z = vec;
	varfloat dr = _1;
	varfloat r = _0;
	t *= power;
	int i;
	for (i = 0; i < DE_Iters; i++) {
		r = length(z);
		if (r > bailout) break;
		
		// convert to polar coordinates
		varfloat theta = acos(z.z/r);
		varfloat phi = atan(z.y/z.x);
		dr =  pow(r, t-_1)*t*dr + _1;
		
		// scale and rotate the point
		varfloat zr = pow(r,t);
		theta = theta*t;
		phi = phi*t;
		
		// convert back to cartesian coordinates
		z = zr*(varfloat3)(sin(theta)*cos(phi), sin(phi)*sin(theta), cos(theta));
		z += vec;
	}
	dist = _0p5*log(r)*r/dr;
	return dist;
}

#define scale -_1p5
#define fixedRadius _1
#define fixedRadius2 _1
#define minRadius _0p5
#define minRadius2 0.25

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

#define foldingLimit _1

void boxFold(varfloat3 *vec) {
	*vec = clamp(*vec, -foldingLimit, foldingLimit) * _2 - *vec;
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
	t += _0p5;
	t *= scale;
	varfloat3 offset = vec;
	varfloat dr = t;//1.0;
	for(int n = 0; n < DE_Iters; n++) {
		boxFold(&vec);       // Reflect
		sphereFold(&vec, &dr);    // Sphere Inversion
 		vec = t*vec + offset;  // Scale & Translate
        dr = dr*fabs(t)+_1;
	}
	return length(vec) / fabs(dr);
}

varfloat2 fold(varfloat2 vec, varfloat ang){
    varfloat2 n = (varfloat2)(cos(-ang), sin(-ang));
    vec -= _2 * min(_0, dot(vec, n)) * n;
    return vec;
}

/*varfloat3 tri_fold(varfloat3 vec, varfloat t) {
    vec.xy = fold(vec.xy, PI/_3 - cos(t)/10.0f);
    vec.xy = fold(vec.xy, -PI/_3);
    vec.yz = fold(vec.yz, -PI/6.0f + sin(t)/_2);
    vec.yz = fold(vec.yz, PI/6.0f);
    return vec;
}

varfloat DE_Koch_t(varfloat3 vec, varfloat t){
    vec *= 0.75f;
    vec.x += _1p5;
    for(int i = 0; i < 8; i++){
        vec *= _2;
        vec.x -= 2.6f;
        vec = tri_fold(vec, t);
    }
    return length( vec*0.004f ) - 0.01f;
}*/

varfloat smoothMin(varfloat dstA, varfloat dstB, varfloat k){
	varfloat h = fmax(k - fabs(dstA - dstB), _0) / k;
	return fmin(dstA, dstB) - h*h*h*k/_6;
}

varfloat DE(varfloat3 position, int DE_Iters, varfloat t, int fractalNum) {
	switch(fractalNum){
		case 0: 
			return DE_Mandelbulb(position, DE_Iters);
		case 1: 
			return DE_Mandelbox(position, DE_Iters); 
		case 2: 
			return DE_Sponge(position, DE_Iters);
		case 3:
			return smoothMin(
						smoothMin(DE_Torus(position, (varfloat3)(0, 3*t-1, 1), (varfloat2)(0.5, 0.25)), DE_Sphere(position, (varfloat3)(-1+3*t, -1+3*t, -_1p5+4*t), _0p25), _1),
						DE_Box(position, (varfloat3)(0, 0, 0), (varfloat3)(_1, _1, _0p5)), _1);
		
		default: 
			return DE_Mandelbox(position, DE_Iters); //default
	}
}





varfloat3 Hue(varfloat hue) { //Hue is from 0 to 1
	hue *= _6;
	varfloat x = _1-fabs(fmod(hue, _2) - _1);
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

#define xDir (varfloat3)(0.001, 0, 0)
#define yDir (varfloat3)(0, 0.001, 0)
#define zDir (varfloat3)(0, 0, 0.001)

varfloat3 reflect(varfloat3 incident, varfloat3 pos, int DE_Iters, varfloat t, int fractalNum) {
	varfloat3 normal = normalize((varfloat3)(DE(pos+xDir, DE_Iters, t, fractalNum)-DE(pos-xDir, DE_Iters, t, fractalNum), 
											 DE(pos+yDir, DE_Iters, t, fractalNum)-DE(pos-yDir, DE_Iters, t, fractalNum),
											 DE(pos+zDir, DE_Iters, t, fractalNum)-DE(pos-zDir, DE_Iters, t, fractalNum)));
	
	
	return incident - _2 * dot(incident, normal) * normal;
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
					 const int AA,					//10
					 const int fractalNum) {		//11

	int2 pixelcoords = {get_global_id(0), get_global_id(1)};
	
	//varfloat y = yaw - fov/2.0 + fov*ix/width;
	//varfloat f = (fov * height) / width;
	//varfloat p = pitch - f/2.0 + f*iy/height;
	//varfloat3 direction = {sin(y)*cos(p), cos(y)*cos(p), sin(p)};
	
	varfloat3 position = {px, py, pz};
	
	varfloat3 colorvec = {0, 0, 0};
	
	varfloat3 tempcolor;
	
	varfloat2 p;
	varfloat3 temp;
	varfloat3 direction;
	int rayiterations;
	varfloat currentDist;
	varfloat raylength;
	int collisions;
	
	varfloat divisor;
	
	for(int m = 0; m < AA; m++) {
		p.x = (-width+_2*(pixelcoords.x + (varfloat)m/(varfloat)AA - _0p5))/(varfloat)height;
		for(int n = 0; n < AA; n++) {
			p.y = (-height+_2*(pixelcoords.y + (varfloat)n/(varfloat)AA - _0p5))/(varfloat)height;
			
			temp = normalize((varfloat3)(p.x, p.y, _2));
			
			position = (varfloat3)(px, py, pz);
			
			tempcolor = (varfloat3)(0, 0, 0);
			
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
			collisions = 0;
			divisor = 0;
			
			while(raylength <= maxraylength && rayiterations < maxrayiterations && collisions < maxcollisions) {
				currentDist = DE(position, DE_Iters, t, fractalNum);
				position += direction * currentDist;
				raylength += currentDist;
				rayiterations++;
				if(currentDist <= collidethresh) {
					direction = reflect(direction, position, DE_Iters, t, fractalNum);
					//divisor += pow(_0p5, collisions);
					tempcolor += Hue(fmod(length(position), _1))*(maxrayiterations-rayiterations)/maxrayiterations;
					collisions++;
				}
			}
			
			
			if(collisions > 0) {
				colorvec += tempcolor / collisions;//divisor;
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
