package com.surevine.alfresco.repo.upload;

import java.net.URLDecoder;

public class FilenameNormaliserImpl implements FilenameNormaliser {

	@Override
	public String normalise(String filename) {
		String s = removeNonBasicASCII(filename);
		
		s = removeMultipleURLEncoding(s);
		
		if (!isDecodable(filename))
		{
			s = removePercents(s); //Vital we do this _after_ removing multiple encodings
		}
		return s;
	}
	
	protected String removePercents(String filename)
	{
		String noPercents = filename.replace('%', '_');
		return noPercents.replaceAll("__+", "_");
	}
	
	protected boolean isDecodable(String filename)
	{
		try
		{
			String decoded=URLDecoder.decode(filename); //We want to know if items are decodable to basic ascii, not unicode chars
			return decoded.equals(removeNonBasicASCII(decoded));		
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	protected String removeNonBasicASCII(String filename)
	{
		//Step through every character in the string, building a new String
		//containing only basic ascii chars, with all sequences of extended or unicode chars replaced with _
		
		StringBuffer out = new StringBuffer(filename.length());
		
		char[] chars = filename.toCharArray();
		
		for (int i=0; i < chars.length; i++)
		{
			if (chars[i]<128)// && chars[i]!=37)
			{
				out.append(chars[i]);
			}
			else
			{
				out.append('_');
			}
		}
		//Replace multiple consecutive _ with a single _ to improve readability
		return out.toString().replaceAll("_+", "_");
	}
	
	protected String removeMultipleURLEncoding(String filename)
	{
		//If we're not encoded at all, or we can't decode, just return the input String
		String decoded=filename;
		try {
			decoded = URLDecoder.decode(filename);
		}
		catch (IllegalArgumentException e)
		{
			
		}
		if (decoded.equals(filename))
		{
			return filename;
		}
		
		String thisString = filename;
		String nextString = decoded;
		
		//We're encoded at least once, but that's OK - keep decoding until the string doesn't change when encoded, then use the String from one step before that
		//(or, to put it another way two steps before the last decode)
		//In case of some sort of error, put a theoretical limit of 500-times encoding on the string
		for (int i=0; i < 500; i++)
		{
			String afterNextString="";
			try 
			{
				afterNextString = URLDecoder.decode(nextString);
			}
			//If the decoding fails with an iae, then it means we've finished decoding but have decoded into a String containing a % charecter, which is fine
			catch (IllegalArgumentException e)
			{
				return thisString;
			}
			if (nextString.equals(afterNextString))
			{
				return thisString;
			}
			thisString=nextString;
			nextString=afterNextString;
		}
		
		return filename;
	}
}