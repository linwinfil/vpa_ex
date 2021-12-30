const hm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			time: {
				type: "f",
				value: 0
			},
			distortion: {
				type: "f",
				value: 3
			},
			distortion2: {
				type: "f",
				value: 5
			},
			speed: {
				type: "f",
				value: .116
			},
			rollSpeed: {
				type: "f",
				value: .05
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: `\n\n\tuniform sampler2D tDiffuse;\n\tuniform float time;\n\tuniform float distortion;\n\tuniform float distortion2;\n\tuniform float speed;\n\tuniform float rollSpeed;\n\tvarying vec2 vUv;\n\t\n\t${um}\n\n\tvoid main() {\n\n\t\tvec2 p = vUv;\n\t\tfloat ty = time * speed * 17.346;\n\t\tfloat yt = p.y - ty;\n\n\t\t//thick distortion\n\t\tfloat offset = noise2d(vec2(yt*3.0,0.0))*0.2;\n\t\toffset = offset*distortion * offset*distortion * offset;\n\t\t//fine distortion\n\t\toffset += noise2d(vec2(yt*50.0,0.0))*distortion2*0.002;\n\t\t\n\t\t//combine distortion on X with roll on Y\n\t\tgl_FragColor = texture2D(tDiffuse,  vec2(fract(p.x + offset),fract(p.y - time * rollSpeed) ));\n\n\t}\n`
	},
	pm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			amount: {
				type: "f",
				value: .5
			},
			time: {
				type: "f",
				value: 0
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float amount;\n\tuniform float time;\n\n\tvarying vec2 vUv;\n\n\tconst int num_iter = 16;\n\tconst float reci_num_iter_f = 1.0 / float(num_iter);\n\tconst float gamma = 2.2;\n\tconst float MAX_DIST_PX = 200.0;\n\n\tvec2 barrelDistortion( vec2 p, vec2 amt )\n\t{\n\t\tp = 2.0*p-1.0;\n\t\t//float BarrelPower = 1.125;\n\t\tconst float maxBarrelPower = 3.0;\n\t\tfloat theta  = atan(p.y, p.x);\n\t\tfloat radius = length(p);\n\t\tradius = pow(radius, 1.0 + maxBarrelPower * amt.x);\n\t\tp.x = radius * cos(theta);\n\t\tp.y = radius * sin(theta);\n\t\treturn 0.5 * ( p + 1.0 );\n\t}\n\n\tfloat sat( float t )\n\t{\n\t\treturn clamp( t, 0.0, 1.0 );\n\t}\n\n\tfloat linterp( float t ) {\n\t\treturn sat( 1.0 - abs( 2.0*t - 1.0 ) );\n\t}\n\n\tfloat remap( float t, float a, float b ) {\n\t\treturn sat( (t - a) / (b - a) );\n\t}\n\n\tvec3 spectrum_offset( float t ) {\n\t\tvec3 ret;\n\t\tfloat lo = step(t,0.5);\n\t\tfloat hi = 1.0-lo;\n\t\tfloat w = linterp( remap( t, 1.0/6.0, 5.0/6.0 ) );\n\t\tret = vec3(lo,1.0,hi) * vec3(1.0-w, w, 1.0-w);\n\t\n\t\treturn pow( ret, vec3(1.0/2.2) );\n\t}\n\n\tfloat nrand( vec2 n )\n\t{\n\t\treturn fract(sin(dot(n.xy, vec2(12.9898, 78.233)))* 43758.5453);\n\t}\n\n\tvec3 lin2srgb( vec3 c )\n\t{\n\t\treturn pow( c, vec3(gamma) );\n\t}\n\n\tvec3 srgb2lin( vec3 c )\n\t{\n\t\treturn pow( c, vec3(1.0/gamma));\n\t}\n\n\tvoid main() {\n\n\t\tvec2 uv = vUv;\n\t\t//resolution independent\n\t\tvec2 max_distort = vec2(amount); \n\n\t\tvec2 oversiz = barrelDistortion( vec2(1,1), max_distort );\n\t\tuv = 2.0 * uv - 1.0;\n\t\tuv = uv / (oversiz*oversiz);\n\t\tuv = 0.5 * uv + 0.5;\n\n\t\tvec3 sumcol = vec3(0.0);\n\t\tvec3 sumw = vec3(0.0);\n\t\tfloat rnd = nrand( uv + fract(time) );\n\t\tfor ( int i=0; i<num_iter;++i ){\n\t\t\tfloat t = (float(i)+rnd) * reci_num_iter_f;\n\t\t\tvec3 w = spectrum_offset( t );\n\t\t\tsumw += w;\n\t\t\tsumcol += w * srgb2lin(texture2D( tDiffuse, barrelDistortion(uv, max_distort*t ) ).rgb);\n\t\t}\n\n\t\tsumcol.rgb /= sumw;\n\t\tvec3 outcol = lin2srgb(sumcol.rgb);\n\t\toutcol += rnd/255.0;\n\t\tgl_FragColor = vec4( outcol, 1.0);\n\t}\n\t"
	},
	dm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			dots: {
				type: "f",
				value: 40
			},
			size: {
				type: "f",
				value: .3
			},
			blur: {
				type: "f",
				value: .3
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tuniform float dots;\n\tuniform float size;\n\tuniform float blur;\n\n\tvarying vec2 vUv;\n\n\tvoid main() {\n\t\tfloat dotSize = 1.0/dots;\n\t\tvec2 samplePos = vUv - mod(vUv, dotSize) + 0.5 * dotSize;\n\t\tfloat distanceFromSamplePoint = distance(samplePos, vUv);\n\t\tvec4 col = texture2D(tDiffuse, samplePos);\n\t\tgl_FragColor = mix(col, vec4(0.0), smoothstep(dotSize * size, dotSize *(size + blur), distanceFromSamplePoint));\n\n\t}\n\t"
	},
	fm = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			colLight: {},
			colDark: {}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform vec3 colLight;\n\tuniform vec3 colDark;\n\n\tvarying vec2 vUv;\n\n\t//get float luma from color\n\tfloat luma(vec3 color) {\n\t\treturn dot(color, vec3(0.299, 0.587, 0.114));\n\t}\n\n\t//boost contrast\n\tvec3 boostContrast(vec3 col, float amount){\n\t\treturn  (col - 0.5) / (1.0 - amount) + 0.5;\n\t}\n\n\tvoid main() {\n\t\tvec3 col =  texture2D(tDiffuse, vUv).rgb;\n\t\t//col += brightness;\n\t\t//col = boostContrast(col,contrast);\n\t\tcol = clamp(col,0.0,1.0);\n\t\tcol = mix(colDark,colLight, luma(col));\n\t\tgl_FragColor = vec4(col,1.0);\n\t}\n"
	},
	mm = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			amount: {
				value: 0
			},
			passthru: {
				value: 0
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tuniform float amount;\n\tuniform float passthru;\n\tvarying vec2 vUv;\n\n\tvec2 texel = vec2(1.0 /512.0);\n\n\tmat3 G[2];\n\n\tconst mat3 g0 = mat3( 1.0, 2.0, 1.0, 0.0, 0.0, 0.0, -1.0, -2.0, -1.0 );\n\tconst mat3 g1 = mat3( 1.0, 0.0, -1.0, 2.0, 0.0, -2.0, 1.0, 0.0, -1.0 );\n\n\n\tvoid main(void)\n\t{\n\t\tmat3 I;\n\t\tfloat cnv[2];\n\t\tvec3 sample;\n\n\t\tG[0] = g0;\n\t\tG[1] = g1;\n\n\t\t/* fetch the 3x3 neighbourhood and use the RGB vectors length as intensity value */\n\t\tfor (float i=0.0; i<3.0; i++)\n\t\tfor (float j=0.0; j<3.0; j++) {\n\t\t\tsample = texture2D( tDiffuse, vUv + texel * vec2(i-1.0,j-1.0) ).rgb;\n\t\t\tI[int(i)][int(j)] = length(sample);\n\t\t}\n\n\t\t/* calculate the convolution values for all the masks */\n\t\tfor (int i=0; i<2; i++) {\n\t\t\tfloat dp3 = dot(G[i][0], I[0]) + dot(G[i][1], I[1]) + dot(G[i][2], I[2]);\n\t\t\tcnv[i] = dp3 * dp3; \n\t\t}\n\n\t\tvec4 orig = texture2D( tDiffuse, vUv);\n\n\t\tgl_FragColor = orig * passthru + vec4(0.5 * sqrt(cnv[0]*cnv[0]+cnv[1]*cnv[1])) * amount;\n\t}\n"
	},
	vm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			amount: {
				type: "f",
				value: .5
			},
			speed: {
				type: "f",
				value: .5
			},
			time: {
				type: "f",
				value: 0
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tvarying vec2 vUv;\n\tuniform float amount;\n\tuniform float speed;\n\tuniform float time;\n\n\tfloat random1d(float n){\n\t\treturn fract(sin(n) * 43758.5453);\n\t}\n\n\t//2D (returns 0 - 1)\n\tfloat random2d(vec2 n) { \n\t\treturn fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);\n\t}\n\n\tfloat randomRange (in vec2 seed, in float min, in float max) {\n\t\treturn min + random2d(seed) * (max - min);\n\t}\n\n\t// return 1 if v inside 1d range\n\tfloat insideRange(float v, float bottom, float top) {\n\treturn step(bottom, v) - step(top, v);\n\t}\n\n\tfloat rand(vec2 co){\n\t\treturn fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n\t}\n\n\tvoid main() {\n\t\t\n\t\tvec2 uv = vUv;\n\n\t\tfloat sTime = floor(time * speed * 6.0 * 24.0);\n\t\tvec3 inCol = texture2D(tDiffuse, uv).rgb;\n\t\t\n\t\t//copy orig\n\t\tvec3 outCol = inCol;\n\t\t\n\t\t//randomly offset slices horizontally\n\t\tfloat maxOffset = amount/2.0;\n\n\t\tvec2 uvOff;\n\t\t\n\t\tfor (float i = 0.0; i < 10.0; i += 1.0) {\n\n\t\t\tif (i > 10.0 * amount) break;\n\n\t\t\tfloat sliceY = random2d(vec2(sTime + amount, 2345.0 + float(i)));\n\t\t\tfloat sliceH = random2d(vec2(sTime + amount, 9035.0 + float(i))) * 0.25;\n\t\t\tfloat hOffset = randomRange(vec2(sTime + amount, 9625.0 + float(i)), -maxOffset, maxOffset);\n\t\t\tuvOff = uv;\n\t\t\tuvOff.x += hOffset;\n\t\t\tvec2 uvOff = fract(uvOff);\n\t\t\tif (insideRange(uv.y, sliceY, fract(sliceY+sliceH)) == 1.0 ){\n\t\t\t\toutCol = texture2D(tDiffuse, uvOff).rgb;\n\t\t\t}\n\t\t}\n\t\n\t\t//do color offset - slight shift on one entire channel\n\t\tfloat maxColOffset = amount/6.0;\n\t\tvec2 colOffset = vec2(randomRange(vec2(sTime + amount, 3545.0),-maxColOffset,maxColOffset), randomRange(vec2(sTime , 7205.0),-maxColOffset,maxColOffset));\n\n\t\tuvOff = fract(uv + colOffset);\n\t\t\n\t\t//TODO - use col[1] array access\n\t\tfloat rnd = random2d(vec2(sTime + amount, 9545.0));\n\t\tif (rnd < 0.33){\n\t\t\toutCol.r = texture2D(tDiffuse, uvOff).r;\n\t\t}else if (rnd < 0.66){\n\t\t\toutCol.g = texture2D(tDiffuse, uvOff).g;\n\t\t} else{\n\t\t\toutCol.b = texture2D(tDiffuse, uvOff).b;\n\t\t}\n\t\tgl_FragColor = vec4(outCol,1.0);\n\t}\n\t"
	},
	gm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			amount: {
				type: "f",
				value: .5
			},
			size: {
				type: "f",
				value: 4
			},
			darkness: {
				type: "f",
				value: .1
			},
			resolution: {
				type: "v2"
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tuniform float size;\n\tuniform float amount;\n\tuniform vec2 resolution;\n\tuniform float darkness;\n\n\tvarying vec2 vUv;\n\n\tvoid main() {\n\n\t\tfloat h = size / resolution.x;\n\t\tfloat v = size / resolution.y;\n\t\t\n\t\tvec4 sum = vec4( 0.0 );\n\n\t\t//H Blur\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x - 4.0 * h, vUv.y ) )- darkness) * 0.051 ;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x - 3.0 * h, vUv.y ) )- darkness) * 0.0918;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x - 2.0 * h, vUv.y ) )- darkness) * 0.12245;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x - 1.0 * h, vUv.y ) )- darkness) * 0.1531;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y ) )- darkness) * 0.1633;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x + 1.0 * h, vUv.y ) )- darkness) * 0.1531;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x + 2.0 * h, vUv.y ) )- darkness) * 0.12245;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x + 3.0 * h, vUv.y ) )- darkness) * 0.0918;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x + 4.0 * h, vUv.y ) )- darkness) * 0.051;\n\t\t\n\t\t//V Blur\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y - 4.0 * v ) )- darkness) * 0.051;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y - 3.0 * v ) )- darkness) * 0.0918;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y - 2.0 * v ) )- darkness) * 0.12245;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y - 1.0 * v ) )- darkness) * 0.1531;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y ) )- darkness) * 0.1633;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y + 1.0 * v ) )- darkness) * 0.1531;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y + 2.0 * v ) )- darkness) * 0.12245;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y + 3.0 * v ) )- darkness) * 0.0918;\n\t\tsum += (texture2D( tDiffuse, vec2( vUv.x, vUv.y + 4.0 * v ) )- darkness) * 0.051;\n\n\t\t//get original pixel color\n\t\tvec4 base = texture2D( tDiffuse, vUv );\n\t\t\n\t\t//Additive Blend\n\t\tgl_FragColor = base + max(sum,0.0) * amount;\n\t}\n\t"
	},
	ym = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			resolution: {
				type: "v2"
			},
			scale: {
				type: "f",
				value: 0
			},
			noiseScale: {
				type: "f",
				value: .1
			},
			centerX: {
				type: "f",
				value: .5
			}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tuniform vec2 resolution;\n\tvarying vec2 vUv;\n\tuniform float scale;\n\tuniform float noiseScale;\n\tuniform float centerX;\n\n\tfloat luma(vec3 color) {\n\t\treturn dot(color, vec3(0.299, 0.587, 0.114));\n\t}\n\n\tvoid main() {\n\n\t\tvec2 center = vec2( 0.5 );\n\t\tcenter.x = centerX;\n\t\tvec2 uv = vUv;\n\n\t\t//float noiseScale = 0.1;\n\t\tfloat radius = 0.5;\n\t\tvec2 d = uv - center;\n\t\tfloat r = length( d * vec2( 1., resolution.y / resolution.x ) ) * scale;\n\t\tfloat a = atan(d.y,d.x) + noiseScale*(radius-r)/radius;\n\t\tvec2 uvt = center+r*vec2(cos(a),sin(a));\n\n\t\tvec2 uv2 = vUv;\n\t\tfloat c = ( .75 + .25 * sin( uvt.x * 1000. ) );\n\t\tvec4 color = texture2D( tDiffuse, uv2 );\n\t\tfloat l = luma( color.rgb );\n\t\tfloat f = smoothstep( .5 * c, c, l );\n\t\tf = smoothstep( 0., .5, f );\n\n\t\tvec3 col = vec3(f);\n\n\t\tgl_FragColor = vec4( col,.0);\n\t}\n\t"
	},
	_m = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			lookupTable: {
				type: "t",
				value: null
			},
			strength: {
				type: "f",
				value: 1
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform sampler2D lookupTable;\n\tuniform float strength;\n\tvarying vec2 vUv;\n\n\tvoid main() {\n\t\tvec4 col = texture2D( tDiffuse, vUv );\n\t\tfloat blueColor = col.b * 63.0;\n\n\t\tvec2 quad1;\n\t\tquad1.y = floor(floor(blueColor) / 8.0);\n\t\tquad1.x = floor(blueColor) - (quad1.y * 8.0);\n\n\t\tvec2 quad2;\n\t\tquad2.y = floor(ceil(blueColor) / 8.0);\n\t\tquad2.x = ceil(blueColor) - (quad2.y * 8.0);\n\n\t\tvec2 texPos1;\n\t\ttexPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * col.r);\n\t\ttexPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * col.g);\n\n\t\t//INVERT\n\t\ttexPos1.y = 1.0-texPos1.y;\n\n\t\tvec2 texPos2;\n\t\ttexPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * col.r);\n\t\ttexPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * col.g);\n\n\t\t//INVERT\n\t\ttexPos2.y = 1.0-texPos2.y;\n\n\t\tvec4 newColor1 = texture2D(lookupTable, texPos1);\n\t\tvec4 newColor2 = texture2D(lookupTable, texPos2);\n\n\t\tvec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n\n\t\tgl_FragColor = mix(col, vec4(newColor.rgb, col.w), strength);\n\t}\n"
	},
	xm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			time: {
				type: "f",
				value: 1
			},
			speed: {
				type: "f",
				value: .5
			},
			scale: {
				type: "f",
				value: .5
			},
			amount: {
				type: "f",
				value: .5
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: `\n\n\tuniform sampler2D tDiffuse;\n\tuniform float time;\n\tuniform float scale;\n\tuniform float amount;\n\tuniform float speed;\n\tvarying vec2 vUv;\n\n\t${um}\n\n\tfloat getNoise(vec2 uv, float t){\n\t\t//generate multi-octave noise based on uv position and time\n\t\t//move noise  over time\n\t\t//scale noise position relative to center\n\t\tuv -= 0.5;\n\t\t//octave 1\n\t\tfloat scl = 4.0 * scale;\n\t\tfloat noise = noise2d( vec2(uv.x * scl ,uv.y * scl - t * speed ));\n\t\t//octave 2\n\t\tscl = 16.0 * scale;\n\t\tnoise += noise2d( vec2(uv.x * scl + t* speed ,uv.y * scl )) * 0.2 ;\n\t\t//octave 3\n\t\tscl = 26.0 * scale;\n\t\tnoise += noise2d( vec2(uv.x * scl + t* speed ,uv.y * scl )) * 0.2 ;\n\t\treturn noise;\n\t}\n\n\tvoid main() {\n\t\tvec2 uv = vUv;\n\t\tfloat noise = getNoise(uv, time * 24.0);\n\t\tvec2 noiseUv = uv + amount * noise;\n\t\t//wrap\n\t\tnoiseUv = fract(noiseUv);\n\t\tgl_FragColor = texture2D(tDiffuse,noiseUv);\n\t}\n`
	},
	bm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			pixelsX: {
				type: "f",
				value: 10
			},
			pixelsY: {
				type: "f",
				value: 10
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float pixelsX;\n\tuniform float pixelsY;\n\tvarying vec2 vUv;\n\n\tvoid main() {\n\n\t\tvec2 p = vUv;\n\t\tp.x = floor(p.x * pixelsX)/pixelsX + 0.5/pixelsX;\n\t\tp.y = floor(p.y * pixelsY)/pixelsY + 0.5/pixelsY;\n\t\tgl_FragColor = texture2D(tDiffuse, p);\n\n\t}\n"
	},
	wm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			pixelsX: {
				type: "f",
				value: .05
			},
			pixelsY: {
				type: "f",
				value: .05
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float pixelsX;\n\tuniform float pixelsY;\n\n\tvarying vec2 vUv;\n\n\tvoid main() {\n\n\t\tvec2 normCoord = 2.0 * vUv - 1.0;\n\t\t// to polar coords\n\t\tfloat r = length(normCoord); \n\t\tfloat phi = atan(normCoord.y, normCoord.x);\n\t\t\t\n\t\tr = r - mod(r, pixelsX) + 0.03;\n\t\tphi = phi - mod(phi, pixelsY);\n\t\t\t\n\t\tnormCoord.x = r * cos(phi);\n\t\tnormCoord.y = r * sin(phi);\n\t\tvec2 textureCoordinateToUse = normCoord / 2.0 + 0.5;\n\t\tgl_FragColor = texture2D(tDiffuse, textureCoordinateToUse );\n\t\n\t}\n"
	},
	Mm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			levels: {
				type: "f",
				value: 4
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tuniform float levels;\n\tvarying vec2 vUv;\n\n\tvoid main() {\n\t\tvec4 col = texture2D( tDiffuse, vUv );\n\t\tgl_FragColor.rgb = floor((col.rgb * levels) + vec3(0.5)) / levels;\n\t}\n"
	},
	Sm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			amount: {
				type: "f",
				value: .5
			},
			offset: {
				type: "f",
				value: .5
			},
			time: {
				type: "f",
				value: .5
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float amount;\n\tuniform float offset;\n\tuniform float time;\n\n\tvarying vec2 vUv;\n\n\tvec3 rainbow2( in float t ){\n\t\tvec3 d = vec3(0.0,0.33,0.67);   \n\t\treturn 0.5 + 0.5*cos( 6.28318*(t+d) );\n\t}\n\n\tvoid main() {\n\t\tvec2 p = vUv;\n\t\tvec3 origCol = texture2D( tDiffuse, p ).rgb;\n\n\t\tvec2 off = texture2D( tDiffuse, p ).rg - 0.5;\n\t\tp += off * offset;\n\t\tvec3 rb = rainbow2( (p.x + p.y + time * 2.0) * 0.5);\n\n\t\tvec3 col = mix(origCol,rb,amount);\n\n\t\tgl_FragColor = vec4(col, 1.0);\n\n\t}\n"
	},
	Am = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			time: {
				value: 0
			},
			noiseAmount: {
				value: .5
			},
			linesAmount: {
				value: .05
			},
			count: {
				value: 4096
			},
			height: {
				value: 4096
			}
		},
		vertexShader: `\n\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float time;\n\tuniform float count;\n\tuniform float noiseAmount;\n\tuniform float linesAmount;\n\tuniform float height;\n\n\tvarying vec2 vUv;\n\n\t#define PI 3.14159265359\n\n\thighp float rand( const in vec2 uv ) {\n\t\tconst highp float a = 12.9898, b = 78.233, c = 43758.5453;\n\t\thighp float dt = dot( uv.xy, vec2( a,b ) ), sn = mod( dt, PI );\n\t\treturn fract(sin(sn) * c);\n\t}\n\n\tvoid main() {\n\n\t\t// sample the source\n\t\tvec4 cTextureScreen = texture2D( tDiffuse, vUv );\n\t\t\n\t\t// add noise\n\t\tfloat dx = rand( vUv + time );\n\t\tvec3 cResult = cTextureScreen.rgb * dx * noiseAmount;\n\t\t\n\t\t// add scanlines\n\t\tfloat lineAmount = height * 1.8 * count;\n\t\tvec2 sc = vec2( sin( vUv.y * lineAmount), cos( vUv.y * lineAmount) );\n\t\tcResult += cTextureScreen.rgb * vec3( sc.x, sc.y, sc.x ) * linesAmount;\n\n\t\t// interpolate between source and result by intensity\n\t\tcResult = cTextureScreen.rgb + ( cResult );\n\n\t\tgl_FragColor =  vec4( cResult, cTextureScreen.a );\n\t}\n"
	},
	Tm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			time: {
				type: "f",
				value: 0
			},
			amount: {
				type: "f",
				value: .05
			}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n\tuniform sampler2D tDiffuse;\n\tuniform float time;\n\tuniform float amount;\n\n\tvarying vec2 vUv;\n\n\tfloat random1d(float n){\n\t\treturn fract(sin(n) * 43758.5453);\n\t}\n\n\tvoid main() {\n\t\tvec2 p = vUv;\n\t\tvec2 offset = (vec2(random1d(time),random1d(time + 999.99)) - 0.5) * amount;\n\t\tp += offset;\n\t\tgl_FragColor = texture2D(tDiffuse, p);\n\t}\n"
	},
	Cm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			slices: {
				type: "f",
				value: 10
			},
			offset: {
				type: "f",
				value: .3
			},
			speedH: {
				type: "f",
				value: .5
			},
			speedV: {
				type: "f",
				value: 1
			},
			time: {
				type: "f",
				value: 0
			}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float slices;\n\tuniform float offset;\n\tuniform float time;\n\tuniform float speedV;\n\tuniform float speedH;\n\tvarying vec2 vUv;\n\n\tfloat steppedVal(float v, float steps){\n\t\treturn floor(v*steps)/steps;\n\t}\n\n\t//RANDOM \n\t//1D\n\t//returns 0 - 1\n\tfloat random1d(float n){\n\t\treturn fract(sin(n) * 43758.5453);\n\t}\n\n\t//returns 0 - 1\n\tfloat noise1d(float p){\n\t\tfloat fl = floor(p);\n\t\tfloat fc = fract(p);\n\t\treturn mix(random1d(fl), random1d(fl + 1.0), fc);\n\t}\n\n\tconst float TWO_PI = 6.283185307179586;\n\n\tvoid main() {\n\t\tvec2 uv = vUv;\n\t\t//variable width strips\n\t\tfloat n = noise1d(uv.y * slices + time * speedV * 3.0);\n\t\tfloat ns = steppedVal(fract(n  ),slices) + 2.0;\n\t\t\n\t\tfloat nsr = random1d(ns);\n\t\tvec2 uvn = uv;\n\t\tuvn.x += nsr * sin(time * TWO_PI + nsr * 20.0) * offset;\n\t\tgl_FragColor = texture2D(tDiffuse, uvn);\n\t}\n"
	},
	Lm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			amount: {
				type: "f",
				value: .5
			},
			time: {
				type: "f",
				value: .5
			}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n\tconst float TWO_PI = 6.283185307179586;\n\n\tuniform sampler2D tDiffuse;\n\tuniform float amount;\n\tuniform float time;\n\n\tvarying vec2 vUv;\n\n\tvec2 rotate2D(vec2 position, float theta){\n\t\tmat2 m = mat2( cos(theta), -sin(theta), sin(theta), cos(theta) );\n\t\treturn m * position;\n\t}\n\n\tvoid main() {\n\t\tvec2 p = vUv;\n\t\t//Displace image by its own rg channel\n\t\tvec2 sPos = vUv;\n\t\tvec2 off = texture2D( tDiffuse, sPos ).rg - 0.5;\n\n\t\t//rotate\n\t\tfloat ang = time * TWO_PI;\n\t\toff = rotate2D(off,ang);\n\t\tp += off * amount;\n\n\t\tvec4 col = texture2D(tDiffuse,p);\n\t\tgl_FragColor = col;\n\t}\n"
	},
	Pm = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			centerBrightness: {
				type: "f",
				value: .5
			},
			powerCurve: {
				type: "f",
				value: 2
			},
			colorize: {
				type: "f",
				value: .1
			}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n    uniform sampler2D tDiffuse;\n\t\t\n    uniform float centerBrightness;\n    uniform float powerCurve;\n    uniform float colorize;\n\n    varying vec2 vUv;\n    \n    vec3 rgb2hsv(vec3 c)\t{\n        vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n        vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);\n        vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);\n        float d = q.x - min(q.w, q.y);\n        float e = 1.0e-10;\n        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n    }\n\n    vec3 hsv2rgb(vec3 c)\t{\n        vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n    }\n\n    void main() {\n        vec3 origCol = texture2D( tDiffuse, vUv ).rgb;\n\n        //\tconvert to HSV\n        vec3 hslColor = rgb2hsv(origCol);\n        vec3 outColor = hslColor;\n        \n        //\tadjust the brightness curve\n        outColor.b = pow(outColor.b, powerCurve);\n        outColor.b = (outColor.b < centerBrightness) ? (1.0 - outColor.b / centerBrightness) : (outColor.b - centerBrightness) / centerBrightness;\n        outColor.g = outColor.g * hslColor.b * colorize;\n        \n        //\tconvert back to rgb\n        outColor = hsv2rgb(outColor);\n        \n        //Additive Blend\n        gl_FragColor = vec4(outColor, 1.0);\n    }\n"
	},
	Em = {
		uniforms: {
			tDiffuse: {
				type: "t",
				value: null
			},
			time: {
				type: "f",
				value: 0
			},
			strength: {
				type: "f",
				value: .001
			},
			size: {
				type: "f",
				value: 50
			},
			speed: {
				type: "f",
				value: 1
			}
		},
		vertexShader: `\n\t\t${cm}\n\t`,
		fragmentShader: "\n\n\tuniform sampler2D tDiffuse;\n\tuniform float time;\n\tuniform float strength;\n\tuniform float size;\n\tuniform float speed;\n\n\tvarying vec2 vUv;\n\n\tconst float TWO_PI = 6.283185307179586;\n\n\tvoid main() {\n\t\tvec2 p = -1.0 + 2.0 * vUv;\n\t\tfloat pos = time * TWO_PI + length(p * size);\n\t\tgl_FragColor = texture2D(tDiffuse, vUv + strength * vec2(cos(pos), sin(pos)));\n\t}\n"
	},
	Om = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			offset: {
				value: 1
			},
			darkness: {
				value: 1
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform float offset;", "uniform float darkness;", "uniform sampler2D tDiffuse;", "varying vec2 vUv;", "void main() {", "vec4 texel = texture2D( tDiffuse, vUv );", "vec2 uv = ( vUv - vec2( 0.5 ) ) * vec2( offset );", "gl_FragColor = vec4( mix( texel.rgb, vec3( 1.0 - darkness ), dot( uv, uv ) ), texel.a );", "}"].join("\n")
	},
	Dm = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			amount: {
				value: .005
			},
			angle: {
				value: 0
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform sampler2D tDiffuse;", "uniform float amount;", "uniform float angle;", "varying vec2 vUv;", "void main() {", "vec2 offset = amount * vec2( cos(angle), sin(angle));", "vec4 cr = texture2D(tDiffuse, vUv + offset);", "vec4 cga = texture2D(tDiffuse, vUv);", "vec4 cb = texture2D(tDiffuse, vUv - offset);", "gl_FragColor = vec4(cr.r, cga.g, cb.b, cga.a);", "}"].join("\n")
	},
	Nm = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			side: {
				value: 1
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform sampler2D tDiffuse;", "uniform int side;", "varying vec2 vUv;", "void main() {", "vec2 p = vUv;", "if (side == 0){", "if (p.x > 0.5) p.x = 1.0 - p.x;", "}else if (side == 1){", "if (p.x < 0.5) p.x = 1.0 - p.x;", "}else if (side == 2){", "if (p.y < 0.5) p.y = 1.0 - p.y;", "}else if (side == 3){", "if (p.y > 0.5) p.y = 1.0 - p.y;", "} ", "vec4 color = texture2D(tDiffuse, p);", "gl_FragColor = color;", "}"].join("\n")
	},
	Im = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			tSize: {
				value: [256, 256]
			},
			center: {
				value: [.5, .5]
			},
			angle: {
				value: 1.57
			},
			scale: {
				value: 1
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform vec2 center;", "uniform float angle;", "uniform float scale;", "uniform vec2 tSize;", "uniform sampler2D tDiffuse;", "varying vec2 vUv;", "float pattern() {", "float s = sin( angle ), c = cos( angle );", "vec2 tex = vUv * tSize - center;", "vec2 point = vec2( c * tex.x - s * tex.y, s * tex.x + c * tex.y ) * scale;", "return ( sin( point.x ) * sin( point.y ) ) * 4.0;", "}", "void main() {", "vec4 color = texture2D( tDiffuse, vUv );", "float average = ( color.r + color.g + color.b ) / 3.0;", "gl_FragColor = vec4( vec3( average * 10.0 - 5.0 + pattern() ), color.a );", "}"].join("\n")
	},
	zm = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			hue: {
				value: 0
			},
			saturation: {
				value: 0
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform sampler2D tDiffuse;", "uniform float hue;", "uniform float saturation;", "varying vec2 vUv;", "void main() {", "gl_FragColor = texture2D( tDiffuse, vUv );", "float angle = hue * 3.14159265;", "float s = sin(angle), c = cos(angle);", "vec3 weights = (vec3(2.0 * c, -sqrt(3.0) * s - c, sqrt(3.0) * s - c) + 1.0) / 3.0;", "float len = length(gl_FragColor.rgb);", "gl_FragColor.rgb = vec3(", "dot(gl_FragColor.rgb, weights.xyz),", "dot(gl_FragColor.rgb, weights.zxy),", "dot(gl_FragColor.rgb, weights.yzx)", ");", "float average = (gl_FragColor.r + gl_FragColor.g + gl_FragColor.b) / 3.0;", "if (saturation > 0.0) {", "gl_FragColor.rgb += (average - gl_FragColor.rgb) * (1.0 - 1.0 / (1.001 - saturation));", "} else {", "gl_FragColor.rgb += (average - gl_FragColor.rgb) * (-saturation);", "}", "}"].join("\n")
	},
	Rm = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			brightness: {
				value: 0
			},
			contrast: {
				value: 0
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform sampler2D tDiffuse;", "uniform float brightness;", "uniform float contrast;", "varying vec2 vUv;", "void main() {", "gl_FragColor = texture2D( tDiffuse, vUv );", "gl_FragColor.rgb += brightness;", "if (contrast > 0.0) {", "gl_FragColor.rgb = (gl_FragColor.rgb - 0.5) / (1.0 - contrast) + 0.5;", "} else {", "gl_FragColor.rgb = (gl_FragColor.rgb - 0.5) * (1.0 + contrast) + 0.5;", "}", "}"].join("\n")
	},
	km = {
		uniforms: {
			tDiffuse: {
				value: null
			},
			v: {
				value: 1 / 512
			},
			r: {
				value: .35
			}
		},
		vertexShader: ["varying vec2 vUv;", "void main() {", "vUv = uv;", "gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );", "}"].join("\n"),
		fragmentShader: ["uniform sampler2D tDiffuse;", "uniform float v;", "uniform float r;", "varying vec2 vUv;", "void main() {", "vec4 sum = vec4( 0.0 );", "float vv = v * abs( r - vUv.y );", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y - 4.0 * vv ) ) * 0.051;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y - 3.0 * vv ) ) * 0.0918;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y - 2.0 * vv ) ) * 0.12245;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y - 1.0 * vv ) ) * 0.1531;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y ) ) * 0.1633;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y + 1.0 * vv ) ) * 0.1531;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y + 2.0 * vv ) ) * 0.12245;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y + 3.0 * vv ) ) * 0.0918;", "sum += texture2D( tDiffuse, vec2( vUv.x, vUv.y + 4.0 * vv ) ) * 0.051;", "gl_FragColor = sum;", "}"].join("\n")
	};