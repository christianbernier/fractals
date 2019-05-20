package com.fractal;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opencl.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
//import java.awt.Font;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeGLX.*;
import static org.lwjgl.glfw.GLFWNativeWGL.*;
import static org.lwjgl.glfw.GLFWNativeX11.*;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10GL.*;
import static org.lwjgl.opencl.KHRGLSharing.*;
import static org.lwjgl.opengl.ARBCLEvent.*;
import static org.lwjgl.opengl.CGL.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.WGL.*;
import static org.lwjgl.opencl.KHRICD.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import com.fractal.graphics.input.*;
//import com.fractal.graphics.fractalEngine.PixelObject;
import static com.fractal.compute.utils.IOUtil.*;
import static com.fractal.compute.utils.InfoUtil.*;
import com.fractal.compute.*;

import com.fractal.graphics.Camera;

public final class RunFractal {
	
	private static final ByteBuffer source;

    static {
        try {
            source = ioResourceToByteBuffer("com/fractal/compute/kernel.cl", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static final ByteBuffer vssource;

    static {
        try {
            vssource = ioResourceToByteBuffer("com/fractal/graphics/graphicsEngine/vertexshader.vs", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static final ByteBuffer fssource;

    static {
        try {
            fssource = ioResourceToByteBuffer("com/fractal/graphics/graphicsEngine/fragmentshader.fs", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
 // OPENCL

    private static IntBuffer errcode_ret;

    private static long platform;
    private static CLCapabilities platformCaps;
    private static int deviceType;
    private static long device;

    private static CLCapabilities deviceCaps;

    private static CLContextCallback clContextCB;
    
    private static Callback debugProc; //warning says unused but it actually is used

    private static long clContext;
    //private final long clColorMap;
    private static long clQueue;
    private static long clProgram;
    private static long clKernel;
    private static long clTexture;
    private static long matrixhandle;
    
    private static double frameNumber = 1;
    private static final double frameLimit = 1000;

    private static final PointerBuffer kernel2DGlobalWorkSize = BufferUtils.createPointerBuffer(2);

    private static boolean doublePrecision = true;

    // OPENGL

    private static int glTexture;

    private static int vao;
    private static int vbo;
    private static int vsh;
    private static int fsh;
    private static int glProgram;

    private static int projectionUniform;
    private static int sizeUniform;

    // VIEWPORT

    private static int fbw, fbh;

    // EVENT SYNCING

    private static final PointerBuffer syncBuffer = BufferUtils.createPointerBuffer(1);

    private static boolean syncGLtoCL; // false if we can make GL wait on events generated from CL queues.
    private static long    clEvent;
    private static long    glFenceFromCLEvent;

    private static boolean syncCLtoGL; // false if we can make CL wait on sync objects generated from GL.
    
    private static boolean shouldInitBuffers = true;
    private static boolean rebuild;
	public static boolean running = false;
	public static boolean play = false;
	public static GLFWWindow window;
	public static int width = 640;
	public static int height = 480;
	private static int maxrayiterations = 100;
	private static int maxDEiterations = 20;
	private static double MOUSECOEFFICIENT = 200;
	private static double KEYBOARDCOEFFICIENT = 0.0002;
	private static final Set<String> params = new HashSet<>(8);
	
	private static final GLFWKeyCallback keyCallback = new KeyboardInput();
	private static final GLFWCursorPosCallback cursorCallback = new MouseInput(width, height);
	private static final GLFWWindowSizeCallback sizeCallback = new SizeInput();
	//private static DoubleBuffer guiAxesCoords = BufferUtils.createDoubleBuffer(6);
	
	private static Camera camera = new Camera(new Vector3D(0, -2.5, 0));
	private static FloatBuffer cameraMatrix = BufferUtils.createFloatBuffer(9);
	
	//private static PixelObject pixels;
	
	public static void init_Graphics() {
		running = true;
		
		glfwSetErrorCallback(GLFWErrorCallback.createPrint());
        if (!glfwInit()) {
            System.out.println("Unable to initialize glfw");
            System.exit(-1);
        }
        
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); //use glfwShowWindow(window.handle); when initialization finishes
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        }
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        List<Long> platforms;

        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            checkCLError(clGetPlatformIDs(null, pi));
            if (pi.get(0) == 0) {
                throw new IllegalStateException("No OpenCL platforms found.");
            }

            PointerBuffer platformIDs = stack.mallocPointer(pi.get(0));
            checkCLError(clGetPlatformIDs(platformIDs, (IntBuffer)null));

            platforms = new ArrayList<>(platformIDs.capacity());

            for (int i = 0; i < platformIDs.capacity(); i++) {
                long           platform = platformIDs.get(i);
                CLCapabilities caps     = CL.createPlatformCapabilities(platform);
                if (caps.cl_khr_gl_sharing || caps.cl_APPLE_gl_sharing) {
                    platforms.add(platform);
                }
            }
        }

        if (platforms.isEmpty()) {
            throw new IllegalStateException("No OpenCL platform found that supports OpenGL context sharing.");
        }

        platforms.sort((p1, p2) -> {
            // Prefer platforms that support GPU devices
            boolean gpu1 = !getDevices(p1, CL_DEVICE_TYPE_GPU).isEmpty();
            boolean gpu2 = !getDevices(p2, CL_DEVICE_TYPE_GPU).isEmpty();
            int     cmp  = gpu1 == gpu2 ? 0 : (gpu1 ? -1 : 1);
            if (cmp != 0) {
                return cmp;
            }

            return getPlatformInfoStringUTF8(p1, CL_PLATFORM_VENDOR).compareTo(getPlatformInfoStringUTF8(p2, CL_PLATFORM_VENDOR));
        });

        platform     = platforms.get(0);
        platformCaps = CL.createPlatformCapabilities(platform);

        String platformID;
        if (platformCaps.cl_khr_icd) {
            platformID = getPlatformInfoStringASCII(platform, CL_PLATFORM_ICD_SUFFIX_KHR); // less spammy
        } else {
            platformID = getPlatformInfoStringUTF8(platform, CL_PLATFORM_VENDOR);
        }

        boolean hasGPU = false;
        for (Long device : getDevices(platform, CL_DEVICE_TYPE_ALL)) {
            long type = getDeviceInfoLong(device, CL_DEVICE_TYPE);
            if (type == CL_DEVICE_TYPE_GPU) {
                hasGPU = true;
            }
        }
        
        deviceType = params.contains("forceCPU") || !hasGPU ? CL_DEVICE_TYPE_CPU : CL_DEVICE_TYPE_GPU;

        String ID = "Realtime Fractal Renderer: " + platformID + " - " + (deviceType == CL_DEVICE_TYPE_CPU ? "CPU" : "GPU");
        
        window = new GLFWWindow(glfwCreateWindow(width, height, ID, NULL, NULL), ID, new CountDownLatch(1));
        
        if(window == null) { 
        	System.out.println("Failed to create window");
        	System.exit(-1);
        }
        
        //-----------------------------------------------------------------------------------------------------
		
		glfwSetInputMode(window.handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetKeyCallback(window.handle, keyCallback);
		glfwSetCursorPosCallback(window.handle, cursorCallback);
		glfwSetWindowSizeCallback(window.handle, sizeCallback);
		
		@SuppressWarnings("unused")
		GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		
		glfwSetWindowPos(window.handle, 100, 100);
	}
	
	public static void init_Compute() {		
        IntBuffer size = BufferUtils.createIntBuffer(2);

        nglfwGetFramebufferSize(window.handle, memAddress(size), memAddress(size) + 4);
        fbw = size.get(0);
        fbh = size.get(1);

        glfwMakeContextCurrent(window.handle);
        GLCapabilities glCaps = GL.createCapabilities();
        if (!glCaps.OpenGL30) {
            throw new RuntimeException("OpenGL 3.0 is required");
        }
        
        if (params.contains("debugGL")) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
            debugProc = GLUtil.setupDebugMessageCallback();
        }
        
        glfwSwapInterval(0);

        errcode_ret = BufferUtils.createIntBuffer(1);

        try {
            // Find devices with GL sharing support
            {
                device = getDevice(platform, platformCaps, deviceType);
                if (device == NULL) {
                    device = getDevice(platform, platformCaps, CL_DEVICE_TYPE_CPU);
                }

                if (device == NULL) {
                    throw new RuntimeException("No OpenCL devices found with OpenGL sharing support.");
                }
                
                deviceCaps = CL.createDeviceCapabilities(device, platformCaps);
            }
            
            // Create the context
            PointerBuffer ctxProps = BufferUtils.createPointerBuffer(7);
            switch (Platform.get()) {
                case WINDOWS:
                    ctxProps
                        .put(CL_GL_CONTEXT_KHR)
                        .put(glfwGetWGLContext(window.handle))
                        .put(CL_WGL_HDC_KHR)
                        .put(wglGetCurrentDC());
                    break;
                case LINUX:
                    ctxProps
                        .put(CL_GL_CONTEXT_KHR)
                        .put(glfwGetGLXContext(window.handle))
                        .put(CL_GLX_DISPLAY_KHR)
                        .put(glfwGetX11Display());
                    break;
                case MACOSX:
                    ctxProps
                        .put(APPLEGLSharing.CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE)
                        .put(CGLGetShareGroup(CGLGetCurrentContext()));
            }
            ctxProps
                .put(CL_CONTEXT_PLATFORM)
                .put(platform)
                .put(NULL)
                .flip();
            clContext = clCreateContext(ctxProps, device, clContextCB = CLContextCallback.create(
                (errinfo, private_info, cb, user_data) -> log(String.format("cl_context_callback\n\tInfo: %s", memUTF8(errinfo)))
            ), NULL, errcode_ret);
            checkCLError(errcode_ret);

            // create command queues for every GPU, init kernels

            // create command queue and upload color map buffer
            clQueue = clCreateCommandQueue(clContext, device, NULL, errcode_ret);
            checkCLError(errcode_ret);

            // load program(s)
            if (deviceType == CL_DEVICE_TYPE_GPU) {
                log("OpenCL Device Type: GPU (Use -forceCPU to use CPU)");
            } else {
                log("OpenCL Device Type: CPU");
            }
            
            log("Display resolution: " + width + "x" + height + " (Use -res <width> <height> to change)");

            log("OpenGL glCaps.GL_ARB_sync = " + glCaps.GL_ARB_sync);
            log("OpenGL glCaps.GL_ARB_cl_event = " + glCaps.GL_ARB_cl_event);

            buildProgram();

            // Detect GLtoCL synchronization method
            syncGLtoCL = !glCaps.GL_ARB_cl_event; // GL3.2 or ARB_sync implied
            log(syncGLtoCL
                ? "GL to CL sync: Using clFinish"
                : "GL to CL sync: Using OpenCL events"
            );

            // Detect CLtoGL synchronization method
            syncCLtoGL = !deviceCaps.cl_khr_gl_event;
            log(syncCLtoGL
                ? "CL to GL sync: Using glFinish"
                : "CL to GL sync: Using implicit sync (cl_khr_gl_event)"
            );

            vao = glGenVertexArrays();
            glBindVertexArray(vao);

            vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);

            try (MemoryStack stack = stackPush()) {
                glBufferData(GL_ARRAY_BUFFER, stack.floats(
                    0.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 1.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 1.0f,
                    1.0f, 1.0f, 1.0f, 1.0f
                ), GL_STATIC_DRAW);
            }

            vsh = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vsh, StandardCharsets.UTF_8.decode(vssource).toString());
            
            glCompileShader(vsh);
            String log = glGetShaderInfoLog(vsh, glGetShaderi(vsh, GL_INFO_LOG_LENGTH));
            if (!log.isEmpty()) {
                log(String.format("VERTEX SHADER LOG: %s", log));
            }

            fsh = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fsh, StandardCharsets.UTF_8.decode(fssource).toString());
            
            glCompileShader(fsh);
            log = glGetShaderInfoLog(fsh, glGetShaderi(fsh, GL_INFO_LOG_LENGTH));
            if (!log.isEmpty()) {
                log(String.format("FRAGMENT SHADER LOG: %s", log));
            }

            glProgram = glCreateProgram();
            glAttachShader(glProgram, vsh);
            glAttachShader(glProgram, fsh);
            glLinkProgram(glProgram);
            log = glGetProgramInfoLog(glProgram, glGetProgrami(glProgram, GL_INFO_LOG_LENGTH));
            if (!log.isEmpty()) {
                log(String.format("PROGRAM LOG: %s", log));
            }

            int posIN = glGetAttribLocation(glProgram, "posIN");
            int texIN = glGetAttribLocation(glProgram, "texIN");

            glVertexAttribPointer(posIN, 2, GL_FLOAT, false, 4 * 4, 0);
            glVertexAttribPointer(texIN, 2, GL_FLOAT, false, 4 * 4, 2 * 4);

            glEnableVertexAttribArray(posIN);
            glEnableVertexAttribArray(texIN);

            projectionUniform = glGetUniformLocation(glProgram, "projection");
            sizeUniform = glGetUniformLocation(glProgram, "size");

            glUseProgram(glProgram);

            glUniform1i(glGetUniformLocation(glProgram, "raymarch"), 0);
        } catch (Exception e) {
            // TODO: cleanup
            throw new RuntimeException(e);
        }

        glDisable(GL_DEPTH_TEST);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        initGLObjects();
        glFinish();

        setKernelConstants();
	}

	public static void handleInput(double timeDelta) {
		glfwPollEvents();
		double m = KEYBOARDCOEFFICIENT * timeDelta;
		if(KeyboardInput.isKeyDown(GLFW_KEY_Q)) {
			System.out.println("Roll Camera CCW");
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_E)) {
			System.out.println("Roll Camera CW");
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_W)) {
			//System.out.println("Move Camera Forward");
			camera.moveRelativeY(m); 
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_S)) {
			//System.out.println("Move Camera Backwards");
			camera.moveRelativeY(-m);
		} 
		if(KeyboardInput.isKeyDown(GLFW_KEY_A)) {
			//System.out.println("Strafe Camera Left");
			camera.moveRelativeX(-m);
		} 
		if(KeyboardInput.isKeyDown(GLFW_KEY_D)) {
			//System.out.println("Strafe Camera Right");
			camera.moveRelativeX(m);
		} 
		if(KeyboardInput.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
			//System.out.println("Move Camera Down");
			camera.moveRelativeZ(-m);
		} 
		if(KeyboardInput.isKeyDown(GLFW_KEY_SPACE)) {
			//System.out.println("Move Camera Up");
			camera.moveRelativeZ(m);
		} 
		if(KeyboardInput.isKeyDown(GLFW_KEY_ESCAPE)) {
			glfwSetInputMode(window.handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_PERIOD)) {
            doublePrecision = !doublePrecision;
            log("DOUBLE PRECISION IS NOW: " + (doublePrecision ? "ON" : "OFF"));
            rebuild = true;
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
			if(KeyboardInput.isKeyDown(GLFW_KEY_EQUAL)) {
				maxrayiterations++;
				System.out.println("Max Ray Iterations: " + maxrayiterations);
			}
			if(KeyboardInput.isKeyDown(GLFW_KEY_MINUS) && maxrayiterations > 0) {
				maxrayiterations--;
				System.out.println("Max Ray Iterations: " + maxrayiterations);
			}
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_RIGHT_CONTROL)) {
			if(KeyboardInput.isKeyDown(GLFW_KEY_EQUAL)) {
				KEYBOARDCOEFFICIENT *= 2.0;
				System.out.println("Movement Speed: " + KEYBOARDCOEFFICIENT);
			}
			if(KeyboardInput.isKeyDown(GLFW_KEY_MINUS) && maxrayiterations > 0) {
				KEYBOARDCOEFFICIENT /= 2.0;
				System.out.println("MOVEMENT SPEED: " + KEYBOARDCOEFFICIENT);
			}
		} else {
			if(KeyboardInput.isKeyDown(GLFW_KEY_EQUAL)) {
				maxDEiterations++;
				System.out.println("Max Fractal Iterations: " + maxDEiterations);
			}
			if(KeyboardInput.isKeyDown(GLFW_KEY_MINUS) && maxDEiterations > 0) {
				maxDEiterations--;
				System.out.println("Max Fractal Iterations: " + maxDEiterations);
			}
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_C)) {
			System.out.println("X" + camera.getLocation().getX() + " Y" + camera.getLocation().getY() + " Z" + camera.getLocation().getZ() + " P" + camera.getPitch()*180.0/Math.PI + " Y" + camera.getYaw()*180.0/Math.PI + " " + camera.getRelativeY());
		}
		if(KeyboardInput.isKeyDown(GLFW_KEY_P)) {
			play = !play;
		}
		
		if(play) {
			System.out.println("Frame Number: " + frameNumber);
		}
		
		
		if(glfwGetMouseButton(window.handle, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
			glfwSetInputMode(window.handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		}
			
		if(Platform.get() == Platform.WINDOWS && (SizeInput.width != width || SizeInput.height != height)) {
			width = SizeInput.width;
			height = SizeInput.height;
			IntBuffer size = BufferUtils.createIntBuffer(2);
			nglfwGetFramebufferSize(window.handle, memAddress(size), memAddress(size) + 4);
	        fbw = size.get(0);
	        fbh = size.get(1);
	        shouldInitBuffers = true;
			//rebuild = true;
		}
		
		if(glfwGetInputMode(window.handle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
			if(-MouseInput.y/MOUSECOEFFICIENT > Math.PI/2.0) {
				glfwSetCursorPos(window.handle, MouseInput.x, -Math.PI/2.0*MOUSECOEFFICIENT);
			} else if(-MouseInput.y/MOUSECOEFFICIENT < -Math.PI/2.0) {
				glfwSetCursorPos(window.handle, MouseInput.x, Math.PI/2.0*MOUSECOEFFICIENT);
			} else {
				camera.setPitch(-MouseInput.y/MOUSECOEFFICIENT);
			}
			camera.setYaw(MouseInput.x/MOUSECOEFFICIENT);
			camera.constructRelativeAxes();
		}
		
		//camera.setAngle(0, -Math.PI/2.0);
		
		//System.out.println(camera + " Time: " + timeDelta + " Width: " + width + " Height: " + height + " X: " + MouseInput.x + " Y: " + MouseInput.y);
//		System.out.println("X" + camera.getLocation().getX() + " Y" + camera.getLocation().getY() + " Z" + camera.getLocation().getZ() + " P" + camera.getPitch()*180.0/Math.PI + " Y" + camera.getYaw()*180.0/Math.PI + " " + camera.getRelativeY());
	}
	
	public static void main(String[] args) {
		parseArgs(args);
		init_Graphics();
		init_Compute();
		glfwShowWindow(window.handle);
		long previousTime = System.currentTimeMillis();
		long timeDelta = 1;
		
		while(running) {
			handleInput(timeDelta);
			render_Compute();
			//render_Graphics();
			
			glfwSwapBuffers(window.handle);
			
			if(glfwWindowShouldClose(window.handle)) {
				running = false;
			}
			
			timeDelta = System.currentTimeMillis() - previousTime;
			previousTime = System.currentTimeMillis();
			
			if(timeDelta < 16) { //60 FPS CAP
				try {
					Thread.sleep(16-timeDelta);
					timeDelta = 16;
				} catch (InterruptedException e) {
					//lol
				}
			}
			
			if(timeDelta <= 0) {
				timeDelta = 1;
			}
			
			if(play) {
				frameNumber++;
				if(frameNumber >= frameLimit) {
					frameNumber = 1;
				}
			}
		}
		
		if (window.signal.getCount() == 0) {
            window.destroy();
            //window = null;
        }

        window.signal.countDown();
		CL.destroy();
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
	}
	
	private static List<Long> getDevices(long platform, int deviceType) {
        List<Long> devices;
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi      = stack.mallocInt(1);
            int       errcode = clGetDeviceIDs(platform, deviceType, null, pi);
            if (errcode == CL_DEVICE_NOT_FOUND) {
                devices = Collections.emptyList();
            } else {
                checkCLError(errcode);

                PointerBuffer deviceIDs = stack.mallocPointer(pi.get(0));
                checkCLError(clGetDeviceIDs(platform, deviceType, deviceIDs, (IntBuffer)null));

                devices = new ArrayList<>(deviceIDs.capacity());

                for (int i = 0; i < deviceIDs.capacity(); i++) {
                    devices.add(deviceIDs.get(i));
                }
            }
        }

        return devices;
    }
	
	public static class GLFWWindow {

		public final long handle;

        public final String ID;

        /** Used to signal that the rendering thread has completed. */
        public final CountDownLatch signal;

        //GLFWWindowSizeCallback      windowsizefun;
        //GLFWFramebufferSizeCallback framebuffersizefun;
        //GLFWKeyCallback             keyfun;
        //GLFWMouseButtonCallback     mousebuttonfun;
        //GLFWCursorPosCallback       cursorposfun;
        //GLFWScrollCallback          scrollfun;

        private GLFWWindow(long handle, String ID, CountDownLatch signal) {
            this.handle = handle;
            this.ID = ID;
            this.signal = signal;
        }

        public void destroy() {
            glfwFreeCallbacks(handle);
            glfwDestroyWindow(handle);
        }

    }
	
	private static long getDevice(long platform, CLCapabilities platformCaps, int deviceType) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            checkCLError(clGetDeviceIDs(platform, deviceType, null, pi));

            PointerBuffer devices = stack.mallocPointer(pi.get(0));
            checkCLError(clGetDeviceIDs(platform, deviceType, devices, (IntBuffer)null));

            for (int i = 0; i < devices.capacity(); i++) {
                long device = devices.get(i);

                CLCapabilities caps = CL.createDeviceCapabilities(device, platformCaps);
                if (!(caps.cl_khr_gl_sharing || caps.cl_APPLE_gl_sharing)) {
                    continue;
                }

                return device;
            }
        }

        return NULL;
    }

    private static void log(String msg) {
        System.err.format("[%s] %s\n", window.ID, msg);
    }

    public static void render_Compute() {
    	try {
        	// make sure GL does not use our objects before we start computing
            if (syncCLtoGL || shouldInitBuffers) {
                glFinish();
            }

            if (shouldInitBuffers) {
                initGLObjects();
                setKernelConstants();
            }

            if (rebuild) {
                buildProgram();
                setKernelConstants();
            }
            computeCL(doublePrecision);

            renderGL();
        } catch (Exception e) {
            e.printStackTrace();
            glfwSetWindowShouldClose(window.handle, true);
        }
    }

    public static void render_Graphics() {
    	
	}
    
    private interface CLReleaseFunction {
        int invoke(long object);
    }

    private static void release(long object, CLReleaseFunction release) {
        if (object == NULL) {
            return;
        }

        int errcode = release.invoke(object);
        checkCLError(errcode);
    }

    public static void cleanup() {
        release(clTexture, CL10::clReleaseMemObject);
        //release(clColorMap, CL10::clReleaseMemObject);

        release(clKernel, CL10::clReleaseKernel);
        release(clProgram, CL10::clReleaseProgram);
        release(clQueue, CL10::clReleaseCommandQueue);
        release(clContext, CL10::clReleaseContext);

        clContextCB.free();

        glDeleteProgram(glProgram);
        glDeleteShader(fsh);
        glDeleteShader(vsh);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);

        /*if (debugProc != null) {
            debugProc.free();
        }*/

        GL.setCapabilities(null);
    }

    // OpenCL

    private static void computeCL(boolean is64bit) {

        kernel2DGlobalWorkSize.put(0, width).put(1, height);

        // start computation
        
        camera.getMatrix(cameraMatrix);
        clEnqueueWriteBuffer(clQueue, matrixhandle, true, 0, cameraMatrix, null, null);
        clSetKernelArg1i(clKernel, 0, width);
        clSetKernelArg1i(clKernel, 1, height);
        clSetKernelArg1i(clKernel, 7, maxrayiterations);
        clSetKernelArg1i(clKernel, 8, maxDEiterations);
        if (!is64bit || !isDoubleFPAvailable(deviceCaps)) {
            clSetKernelArg1f(clKernel, 3, (float)camera.getLocation().getX());
            clSetKernelArg1f(clKernel, 4, (float)camera.getLocation().getY());
            clSetKernelArg1f(clKernel, 5, (float)camera.getLocation().getZ());
            clSetKernelArg1f(clKernel, 9, (float) (frameNumber/frameLimit));
        } else {	
            clSetKernelArg1d(clKernel, 3, camera.getLocation().getX());	
            clSetKernelArg1d(clKernel, 4, camera.getLocation().getY());
            clSetKernelArg1d(clKernel, 5, camera.getLocation().getZ());
            clSetKernelArg1d(clKernel, 9, frameNumber/frameLimit);
        }

        // acquire GL objects, and enqueue a kernel with a probe from the list
        int errcode = clEnqueueAcquireGLObjects(clQueue, clTexture, null, null);
        checkCLError(errcode);

        errcode = clEnqueueNDRangeKernel(clQueue, clKernel, 2,
            null,
            kernel2DGlobalWorkSize,
            null,
            null, null);
        checkCLError(errcode);

        errcode = clEnqueueReleaseGLObjects(clQueue, clTexture, null, !syncGLtoCL ? syncBuffer : null);
        checkCLError(errcode);

        if (!syncGLtoCL) {
            clEvent = syncBuffer.get(0);
            glFenceFromCLEvent = glCreateSyncFromCLeventARB(clContext, clEvent, 0);
        }

        // block until done (important: finish before doing further gl work)
        if (syncGLtoCL) {
            errcode = clFinish(clQueue);
            checkCLError(errcode);
        }
    }

    // OpenGL

    private static void renderGL() {
        glClear(GL_COLOR_BUFFER_BIT);

        //draw slices

        if (!syncGLtoCL) {
            if (glFenceFromCLEvent != NULL) {
                GL32.glWaitSync(glFenceFromCLEvent, 0, 0);
                GL32.glDeleteSync(glFenceFromCLEvent);
                glFenceFromCLEvent = NULL;
            }

            int errcode = clReleaseEvent(clEvent);
            clEvent = NULL;
            checkCLError(errcode);
        }

        glBindTexture(GL_TEXTURE_2D, glTexture);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    private static boolean isDoubleFPAvailable(CLCapabilities caps) {
        return caps.cl_khr_fp64 || caps.cl_amd_fp64;
    }

    private static void buildProgram() {
        if (clProgram != NULL) {
            int errcode = clReleaseProgram(clProgram);
            checkCLError(errcode);
        }

        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        PointerBuffer lengths = BufferUtils.createPointerBuffer(1);

        strings.put(0, source);
        lengths.put(0, source.remaining());

        clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
        checkCLError(errcode_ret);

        CountDownLatch latch = new CountDownLatch(1);

        // disable 64bit floating point math if not available
        StringBuilder options = new StringBuilder(""); //-D USE_TEXTURE
        //options.append(" -w");
        if (doublePrecision && isDoubleFPAvailable(deviceCaps)) {
            //cl_khr_fp64
            options.append(" -D DOUBLE_FP");

            // AMD's version of double precision floating point math
            if (!deviceCaps.cl_khr_fp64 && deviceCaps.cl_amd_fp64) {
                options.append(" -D AMD_FP");
            }
        }

        log("OpenCL COMPILER OPTIONS: " + options);

        CLProgramCallback buildCallback;
        int errcode = clBuildProgram(clProgram, device, options, buildCallback = CLProgramCallback.create((program, user_data) -> {
            log(String.format(
                "The cl_program [0x%X] was built %s",
                program,
                getProgramBuildInfoInt(program, device, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"
            ));
            String log = getProgramBuildInfoStringASCII(program, device, CL_PROGRAM_BUILD_LOG);
            if (!log.isEmpty()) {
                log(String.format("BUILD LOG:\n----\n%s\n-----", log));
            }

            latch.countDown();
        }), NULL);
        checkCLError(errcode);

        // Make sure the program has been built before proceeding
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        buildCallback.free();
        rebuild = false;

        // init kernel with constants
        clKernel = clCreateKernel(clProgram, "raymarch", errcode_ret);
        checkCLError(errcode_ret);
        
        //create camera matrix buffer
        matrixhandle = clCreateBuffer(clContext, CL_MEM_READ_ONLY, 128, errcode_ret);
        checkCLError(errcode_ret);
    }

    private static void initGLObjects() {
        if (clTexture != NULL) {
            checkCLError(clReleaseMemObject(clTexture));
            glDeleteTextures(glTexture);
        }

        glTexture = glGenTextures();

        // Init textures
        glBindTexture(GL_TEXTURE_2D, glTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8UI, width, height, 0, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, (ByteBuffer)null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        clTexture = clCreateFromGLTexture2D(clContext, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, 0, glTexture, errcode_ret);
        checkCLError(errcode_ret);
        glBindTexture(GL_TEXTURE_2D, 0);

        glViewport(0, 0, fbw, fbh);

        glUniform2f(sizeUniform, width, height);

        FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(4 * 4);
        glOrtho(0.0f, width, 0.0f, height, 0.0f, 1.0f, projectionMatrix);
        glUniformMatrix4fv(projectionUniform, false, projectionMatrix);

        shouldInitBuffers = false;
    }

    private static void glOrtho(float l, float r, float b, float t, float n, float f, FloatBuffer m) {
        m.put(new float[] {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        });
        m.flip();

        m.put(0 * 4 + 0, 2.0f / (r - l));
        m.put(1 * 4 + 1, 2.0f / (t - b));
        m.put(2 * 4 + 2, -2.0f / (f - n));

        m.put(3 * 4 + 0, -(r + l) / (r - l));
        m.put(3 * 4 + 1, -(t + b) / (t - b));
        m.put(3 * 4 + 2, -(f + n) / (f - n));
    }

    // init kernels with constants

    private static void setKernelConstants() {
    	clSetKernelArg1p(clKernel, 2, clTexture);
    	clSetKernelArg1p(clKernel, 6, matrixhandle);
    }
    
    private static void parseArgs(String... args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.charAt(0) != '-' && arg.charAt(0) != '/') {
                throw new IllegalArgumentException("Invalid command-line argument: " + args[i]);
            }

            String param = arg.substring(1);

            if ("forceCPU".equalsIgnoreCase(param)) {
                params.add("forceCPU");
            } else if ("debugGL".equalsIgnoreCase(param)) {
                params.add("debugGL");
            } else if ("iterations".equalsIgnoreCase(param)) {
                
            } else if ("res".equalsIgnoreCase(param)) {
                if (args.length < i + 2 + 1) {
                    throw new IllegalArgumentException("Invalid res argument specified.");
                }

                try {
                    width = Integer.parseInt(args[++i]);
                    height = Integer.parseInt(args[++i]);

                    if (width < 1 || height < 1) {
                        throw new IllegalArgumentException("Invalid res dimensions specified.");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid res dimensions specified.");
                }
            }
        }
    }
}
