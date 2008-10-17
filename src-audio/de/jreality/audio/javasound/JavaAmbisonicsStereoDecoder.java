package de.jreality.audio.javasound;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Mixer.Info;

import de.jreality.audio.AmbisonicsVisitor;
import de.jreality.scene.Viewer;

/**
 * 
 * An Ambisonics Decoder which renders into the default JavaSound stereo output, 44.1 kHz, 16 bit signed PCM.
 * Use the {@code launch}-method to activate this renderer for a given {@link Viewer}.
 * 
 * @author <a href="mailto:weissman@math.tu-berlin.de">Steffen Weissmann</a>
 *
 */
public class JavaAmbisonicsStereoDecoder {

	private static final boolean LIMIT = true;
	
	private static final float W_SCALE = (float) Math.sqrt(0.5);
	private static final float Y_SCALE = 0.5f;

	private static final boolean BIG_ENDIAN = false;

	SourceDataLine stereoOut;
	byte[] buffer;
	float[] fbuffer;
	float[] fbuffer_lookAhead;
	
	private int byteLen;
	private int framesize;

	public JavaAmbisonicsStereoDecoder(int framesize) throws LineUnavailableException {

		this.framesize = framesize;
		byteLen = framesize * 2 * 2; // 2 channels, 2 bytes per sample
		buffer = new byte[byteLen];
		fbuffer = new float[2*framesize]; // 2 channels
		fbuffer_lookAhead = new float[2*framesize];
		
		Info[] mixerInfos = AudioSystem.getMixerInfo();
		System.out.println(Arrays.toString(mixerInfos));
		Info info = mixerInfos[0];
		Mixer mixer = AudioSystem.getMixer(info);
		mixer.open();

		AudioFormat audioFormat = new AudioFormat(
					44100, // the number of samples per second
					16, // the number of bits in each sample
					2, // the number of channels
					true, // signed/unsigned PCM
					BIG_ENDIAN); // big endian ?
		
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		if (!mixer.isLineSupported(dataLineInfo)) {
			throw new RuntimeException("no source data line found.");
		}
	
		stereoOut = (SourceDataLine) mixer.getLine(dataLineInfo);

		//stereoOut.open(audioFormat);
		stereoOut.open(audioFormat, 2*byteLen);
		System.out.println("stereoOut bufferSize="+stereoOut.getBufferSize());
		stereoOut.start();
	}

	public void renderAmbisonics(float[] wBuf, float[] xBuf, float[] yBuf, float[] zBuf) {
		if (LIMIT) renderAmbisonicsLimited(wBuf, xBuf, yBuf, zBuf);
		else renderAmbisonicsPlain(wBuf, xBuf, yBuf, zBuf);
	}
		

	private static final double RELEASE_FACTOR = 0.99;
	private static final int HOLD_COUNT = 16;
	float maxSignal=1f;
	int holdcnt;
	
	public void renderAmbisonicsLimited(float[] wBuf, float[] xBuf, float[] yBuf, float[] zBuf) {
		
		float nextFrameMaxSignal = maxSignal;
		
		for (int i = 0; i < framesize; i++) {
			float w = wBuf[i] * W_SCALE;
			float y = yBuf[i] * Y_SCALE;
			fbuffer_lookAhead[2*i]=w+y;
			fbuffer_lookAhead[2*i+1]=w-y;
			float abs = Math.abs(w)+Math.abs(y);
			if (abs > nextFrameMaxSignal) {
				nextFrameMaxSignal=abs;
				holdcnt=HOLD_COUNT;
			}
		}
		
		boolean rampUp = (nextFrameMaxSignal > maxSignal);

		float delta = Math.abs(nextFrameMaxSignal-maxSignal);
		
		float dd=0;
		if (!rampUp) {
			if (holdcnt == 0 && maxSignal > 1f) {
				//start ramp down...
				delta = - (float) (maxSignal*(1-RELEASE_FACTOR));
			} else {
				holdcnt--;
			}
		} else {
			dd = delta/framesize;
		}
		
		for (int i=0; i<framesize; i++) {
			if (maxSignal >= 1) maxSignal+=dd;
			else maxSignal = 1;
			fbuffer[2*i]/=maxSignal;
			fbuffer[2*i+1]/=maxSignal;
		}
		floatToByte(buffer, fbuffer);
		stereoOut.write(buffer, 0, byteLen);
		
		// swap buffers
		float[] tmpF = fbuffer;
		fbuffer = fbuffer_lookAhead;
		fbuffer_lookAhead = tmpF;
	}
	
	public void renderAmbisonicsPlain(float[] wBuf, float[] xBuf, float[] yBuf, float[] zBuf) {
		for (int i = 0; i < framesize; i++) {
			float w = wBuf[i] * W_SCALE;
			float y = yBuf[i] * Y_SCALE;
			fbuffer[2*i]=w+y;
			fbuffer[2*i+1]=w-y;
		}
		floatToByte(buffer, fbuffer);
		stereoOut.write(buffer, 0, byteLen);
	}
	
    /**
	 * Convert float array to byte array.
	 * 
	 * @param byteSound
	 *            User provided byte array to return result in.
	 * @param dbuf
	 *            User provided float array to convert.
	 */
	static final public void floatToByte(byte[] byteSound, float[] dbuf) {
		int bufsz = dbuf.length;
		int ib = 0;
		if (BIG_ENDIAN) {
			for (int i = 0; i < bufsz; i++) {
				short y = (short) (32767. * dbuf[i]);
				byteSound[ib] = (byte) (y >> 8);
				ib++;
				byteSound[ib] = (byte) (y & 0x00ff);
				ib++;
			}
		} else {
			for (int i = 0; i < bufsz; i++) {
				short y = (short) (32767. * dbuf[i]);
				byteSound[ib] = (byte) (y & 0x00ff);
				ib++;
				byteSound[ib] = (byte) (y >> 8);
				ib++;
			}
		}
	}

	
	public int getSamplerate() {
		return 44100;
	}
	
	public static void launch(Viewer viewer) throws LineUnavailableException {
		
		final int frameSize = 1024;

		final JavaAmbisonicsStereoDecoder dec = new JavaAmbisonicsStereoDecoder(frameSize);
		final AmbisonicsVisitor ambiVisitor = new AmbisonicsVisitor(dec.getSamplerate());
		ambiVisitor.setRoot(viewer.getSceneRoot());
		ambiVisitor.setMicrophonePath(viewer.getCameraPath());


		final float bw[] = new float[frameSize];
		final float bx[] = new float[frameSize];
		final float by[] = new float[frameSize];
		final float bz[] = new float[frameSize];
		
		Runnable soundRenderer = new Runnable() {
			public void run() {
				while (true) {
					ambiVisitor.collateAmbisonics(bw, bx, by, bz, frameSize);
					dec.renderAmbisonics(bw, bx, by, bz);
				}
			}
		};

		Thread soundThread = new Thread(soundRenderer);
		soundThread.setName("jReality audio renderer");
		soundThread.setPriority(Thread.MAX_PRIORITY);
		soundThread.start();
	}

}