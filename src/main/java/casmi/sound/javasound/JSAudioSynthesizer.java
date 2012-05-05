 /*
 *   casmi
 *   http://casmi.github.com/
 *   Copyright (C) 2011, Xcoo, Inc.
 *
 *  casmi is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package casmi.sound.javasound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.SourceDataLine;

import org.tritonus.share.sampled.FloatSampleBuffer;

import casmi.sound.AudioEffect;
import casmi.sound.AudioListener;
import casmi.sound.AudioSignal;
import casmi.sound.Sound;
import casmi.sound.spi.AudioSynthesizer;

final class JSAudioSynthesizer extends Thread implements AudioSynthesizer
{
	private AudioListener		listener;
	private AudioSignal			signal;
	private AudioEffect			effect;

	private SourceDataLine		line;
	private AudioFormat			format;
	private FloatSampleBuffer	buffer;
	private int						bufferSize;
	private boolean				finished;
	private byte[]					outBytes;

	JSAudioSynthesizer(SourceDataLine sdl, int bufferSize)
	{
		super();
		this.bufferSize = bufferSize;
		format = sdl.getFormat();

		buffer = new FloatSampleBuffer(format.getChannels(), bufferSize,
													format.getSampleRate());
		outBytes = new byte[buffer.getByteArrayBufferSize(format)];
		finished = false;
		line = sdl;
	}

	public void run()
	{
		line.start();
		while (!finished)
		{
			buffer.makeSilence();
			if (line.getFormat().getChannels() == Sound.MONO)
			{
				// mrr, don't like this, but it's necessary because of how
				// the constructor for AudioSource works.
				if (signal != null)
				{
					signal.generate(buffer.getChannel(0));
				}
				effect.process(buffer.getChannel(0));
				listener.samples(buffer.getChannel(0));
			}
			else
			{
				if (signal != null)
				{
					signal.generate(buffer.getChannel(0), buffer.getChannel(1));
				}
				effect.process(buffer.getChannel(0), buffer.getChannel(1));
				listener.samples(buffer.getChannel(0), buffer.getChannel(1));
			}
			buffer.convertToByteArray(outBytes, 0, format);
      if ( line.available() == line.getBufferSize() )
      {
        Sound.error("Likely buffer underrun in AudioOutput.");
      }
      line.write(outBytes, 0, outBytes.length);
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
			}
		}
		line.drain();
		line.stop();
		line.close();
		line = null;
	}

	public void open()
	{
		start();
	}

	public void close()
	{
		finished = true;
	}

	public int bufferSize()
	{
		return bufferSize;
	}

	public AudioFormat getFormat()
	{
		return format;
	}

	public void setAudioEffect(AudioEffect effect)
	{
		this.effect = effect;
	}

	public void setAudioSignal(AudioSignal signal)
	{
		this.signal = signal;
	}

	public void setAudioListener(AudioListener listener)
	{
		this.listener = listener;
	}

	public Control[] getControls()
	{
		return line.getControls();
	}
}
