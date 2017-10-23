package net.floodlightcontroller.congestionControl;

public class RTPStaticPT 
{


	static char[] BiVersion = new char[2];//binary kind of version
	static char[] Padding = new char[1];
	static char[] Extension = new char[1];
	static char[] BiCC = new char[4];
	static char[] Marker = new char[1];
	static char[] BiPT = new char[7];
	static char[] BiSeqNumb = new char[16];
	static char[] BiTimeStamp = new char[32];
	static char[] BiSSRCIdentifier = new char[32];
	static char[] BiCSRCIdentifier = new char[32];





	static String DecVersion; //decimal kind of version
	static String DecCC;
	static int DecPT;
	static String TypeOfPayload;
	static String DecSeqNumb;
	static String DecTimeStamp;
	static String DecSSRCIdentifier;
	static String DecCSRCIdentifier;

	public static RTPHeaderFieldReturn RTPVersionRecognizer(StringBuilder sb)
	{
		for( int i = 0, j = 0; i < sb.length(); i++)
		{
			if( i == 0 || i == 1)
			{
				BiVersion[i] = sb.charAt(i);
			}

			if(i == 2)
			{
				Padding[0] = sb.charAt(i);
			}

			if(i == 3)
			{
				Extension[0] = sb.charAt(i);
			}

			if( i > 3 && i < 8)
			{
				BiCC[j] = sb.charAt(i);
				j++;
				if(j == 4)
				{
					j = 0;
				}
			}

			if(i == 8)
			{
				Marker[0] = sb.charAt(i);
			}

			if( i > 8 && i < 16)
			{
				BiPT[j] = sb.charAt(i);
				j++;
				if(j == 7)
				{
					j = 0;
				}
			}

			if( i > 15 && i < 32)
			{
				BiSeqNumb[j] = sb.charAt(i);
				j++;
				if(j == 16)
				{
					j = 0;
				}
			}

			if( i > 31 && i < 64)
			{
				BiTimeStamp[j] = sb.charAt(i);
				j++;
				if(j == 32)
				{
					j = 0;
				}
			}


			if( i > 63 && i < 96)
			{

				BiSSRCIdentifier[j] = sb.charAt(i);
				j++;
				if(j == 32)
				{
					j = 0;
				}
			}

			if( i > 95 && i < 118)
			{
				BiCSRCIdentifier[j] = sb.charAt(i);
				j++;
				if(j == 32)
				{
					j = 0;
				}
			}



		}


		DecCC = Integer.toString(Integer.parseInt(new String(BiCC), 2));
		DecVersion = Integer.toString(Integer.parseInt(new String(BiVersion), 2));
		DecPT = Integer.parseInt(new String(BiPT), 2);
		DecSeqNumb = Integer.toString(Integer.parseInt(new String(BiSeqNumb), 2));
		DecTimeStamp = Long.toString(Long.parseLong(new String(BiTimeStamp), 2));
		DecSSRCIdentifier = Long.toString(Long.parseLong(new String(BiSSRCIdentifier), 2));
		//		DecCSRCIdentifier = Long.toString(Long.parseLong(new String(BiCSRCIdentifier), 2));


		if(DecPT < 24)
		{
			TypeOfPayload = "Audio";
		}

		else if((DecPT > 23 && DecPT < 33) || (DecPT == 34))
		{
			TypeOfPayload = "Video";
		}

		else if(DecPT == 33)
		{
			TypeOfPayload = "Audio and Video";
		}


		else
		{
			TypeOfPayload = "dynamic type";
		}


		RTPHeaderFieldReturn returnObj = new RTPHeaderFieldReturn();

		returnObj.Version = DecVersion;
		returnObj.Padding = new String(Padding);
		returnObj.Extension = new String(Extension);
		returnObj.CC = DecCC;
		returnObj.Marker = new String(Marker);
		returnObj.PT = TypeOfPayload;
		returnObj.SeqNumb = Integer.parseInt(DecSeqNumb);
		//		returnObj.timeStamp = Integer.parseInt(DecTimeStamp);
		return returnObj;

	}


}
