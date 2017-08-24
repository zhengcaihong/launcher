#include "IconGenerator.h"
#include "LinearAlgebraic.h"
#include "QuadraticCurveFitting.h"
#include "stdlib.h"
#include "stdio.h"
#include "math.h"

#include <android/log.h>
#include <sys/time.h>

static char showBuffer[256];
static float timeuse;

static timeval start;
static timeval end;

void GetCenter( unsigned char *big, unsigned char *small, int smallWidth, int smallHeight, int bigWidth, int bandSize, int threshold )
{
	int ind1 = 0;
	int ind2 = 0;
	int ind3 = 0;

	sprintf(showBuffer,"new size %d %d, %d %d  getcenter threshold %d\n",smallWidth, smallHeight, bandSize,bigWidth, threshold );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );


	int lrBand = (bigWidth - smallWidth) / 2;
		
		for ( ind1 = bandSize; ind1 < bandSize+smallHeight; ind1++ )
		{
			for ( ind2 = lrBand; ind2 < lrBand+smallWidth; ind2++ )
			{
				if ( threshold <= small[(ind1-bandSize)*smallWidth*4+(ind2-lrBand)*4+3]  )//////////不透明的地方才赋值
				{
					for ( ind3 = 0; ind3 < 4; ind3++ )/////////透明度也是这个值啊
					{
						big[ind1*bigWidth*4+ind2*4+ind3] = small[(ind1-bandSize)*smallWidth*4+(ind2-lrBand)*4+ind3];
					}
				}
			}
		}

}


void getHis( int val, int shift, int bn, int*p )
{

	p[0]  = val >> shift;
	int temp = val % 32;


	/*
	if ( temp >= 16 )
	{
		if ( val+bn <= 255 )
		{
			p[1] =p[0]+1;
			p[2] = 2;
		}else
		{
			p[2] = 1;
		}
	}else
	{
		if ( val - bn > 0 )
		{
			p[1] = p[0] - 1;
			p[2] = 2;
		}else
		{
			p[2] = 1;
		}
	}
	*/
	///////////////更直观更严格的方式
	if ( temp >= 16 )
	{
		if (p[0]+1 <= 7 )
		{
			p[1] =p[0]+1;
			p[2] = 2;
		}else
		{
			p[2] = 1;
		}
	}else
	{
		if ( p[0] - 1 >= 0 )
		{
			p[1] = p[0] - 1;
			p[2] = 2;
		}else
		{
			p[2] = 1;
		}
	}

	

}

int paraCheck(  unsigned char *small, int smallWidth,  int smallHeight, int bigWidth, int bigHeight, int bandSize )
{
	int ind1 = 0;
	int ind2 = 0; 
	int ind3 = 0;


	sprintf(showBuffer, "paraCheck  %d %d, %d %d , %d\n", smallWidth, smallHeight, bigWidth, bigHeight, bandSize );
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);


	int maxWidth = 0;
	int maxInd = 0;
	int minInd = 0;

	for ( ind1 = 0; ind1 < smallHeight; ind1++ )
	{
		maxInd = 0;
		minInd = 0;
		for ( ind2 = 0; ind2 < smallWidth; ind2++ )
		{
			if (small[ ind1*smallWidth * 4+ ind2* 4 + 3] > 0 )
			{
				minInd = ind2;
				break;
			}

		}

		for ( ind2 = smallWidth-1; ind2 >= 0; ind2--)
		{
			if (small[ ind1*smallWidth * 4+ ind2* 4 + 3] > 0 )
			{
				maxInd = ind2;
				break;
			}
		}

		if ( maxInd-minInd+1 > maxWidth )
		{
			maxWidth = maxInd-minInd+1;
		}

	}

	sprintf(showBuffer, "paracheck maxWidth = %d \n", maxWidth );
	__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

	if ( maxWidth > bigWidth )
	{
		sprintf(showBuffer, "paracheck maxWidth=%d >= bigWidth=%d \n", maxWidth,bigWidth );
		__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);
		return 0;
	}

	if ( smallHeight + bandSize > bigHeight )
	{
		sprintf(showBuffer, "paracheck smallHeight + bandSize >= bigHeight %d %d %d\n", smallHeight ,bandSize , bigHeight );
		__android_log_write(ANDROID_LOG_DEBUG, "icongenerator", showBuffer);

		return 0;
	}

	return 1;

}

int Generator( unsigned char *small, int *paras, unsigned char *big )
{
	int ind1 = 0;
	int ind2 = 0;
	int ind3 = 0;
	int intVal = 0;

	float inlinerRatio = 0.52;//0.58

	int smallWidth = paras[0];
	int smallHeight = paras[1];


	int bandSize = paras[2];

	int bigWidth = paras[3];
	int bigHeight = paras[4];

	int fitFlag1 = 0;
	int fitFlag2 = 0;
	int fitFlag3 = 0;
	int pixFlag  = 0;
	int allFitFlag = 0;
        
	sprintf(showBuffer,"new size %d %d, %d %d %d woca\n",smallWidth,smallHeight, bandSize,bigWidth, bigHeight );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	if ( 0 == paraCheck( small, smallWidth,smallHeight,bigWidth,bigHeight,bandSize ) )
	{
		unsigned char colorB = 255;
		unsigned char colorG = 255;
		unsigned char colorR = 255;

		int pixNum = 0;
		unsigned char *ptemp;
		pixNum = bigWidth * bigHeight;
		ptemp = big;
		for (ind1 = 0; ind1 < pixNum; ind1++) {
			*ptemp++ = colorB;
			*ptemp++ = colorG;
			*ptemp++ = colorR;
			*ptemp++ = 255;

		}

		return 0;

	}

	int acCouner  = 1;
	int acNum = 0;
	for ( ind1 = 0; ind1 < smallHeight ; ind1++ )
	{
		for ( ind2 = 0; ind2 < smallWidth ; ind2++ )
		{
			if ( 0 != small[ind1*smallWidth*4+ind2*4+3] )
			{
				acNum += small[ind1*smallWidth*4+ind2*4+3];
				acCouner++;
			}
		}
	}

	acNum /= acCouner;
	
	/*
	
    char *name = "//mnt//sdcard//alpha.txt";
	remove(name);
	FILE *fp = fopen( name, "a" );
	for ( ind1 = 0; ind1 < smallHeight ; ind1++ )
	{
		for ( ind2 = 0; ind2 < smallWidth ; ind2++ )
		{
			fprintf( fp, "%d ", small[ind1*smallWidth*4+ind2*4+3] );
		}
		fprintf( fp, "\n" );
	}
	fclose( fp );
	


	
	name = "//mnt//sdcard//adata.txt";
	remove(name);
	fp = fopen( name, "a" );
	for ( ind1 = 0; ind1 < smallHeight ; ind1++ )
	{
		for ( ind2 = 0; ind2 < smallWidth ; ind2++ )
		{
			fprintf( fp, "%d ", small[ind1*smallWidth*4+ind2*4+0] );
		}
		fprintf( fp, "\n" );

	}
	fclose( fp );

	name = "//mnt//sdcard//bdata.txt";
	remove(name);
	fp = fopen( name, "a" );
	for ( ind1 = 0; ind1 < smallHeight ; ind1++ )
	{
		for ( ind2 = 0; ind2 < smallWidth ; ind2++ )
		{
			fprintf( fp, "%d ", small[ind1*smallWidth*4+ind2*4+1] );
		}
		fprintf( fp, "\n" );
	}
	fclose( fp );

	name = "//mnt//sdcard//cdata.txt";
	remove(name);
	fp = fopen( name, "a" );
	for ( ind1 = 0; ind1 < smallHeight ; ind1++ )
	{
		for ( ind2 = 0; ind2 < smallWidth ; ind2++ )
		{
			fprintf( fp, "%d ", small[ind1*smallWidth*4+ind2*4+2] );
		}
		fprintf( fp, "\n" );
	}
	fclose( fp );
	*/


	int initflag = LineInit( bandSize );

	if ( 1 == initflag )
	{
		pixFlag = GetPixVal( small, smallWidth, smallHeight, smallWidth*4, 4 );
	}else
	{
		pixFlag = 0;
	}

	

	

	fitFlag1 = 0;
	fitFlag2 = 0;
	fitFlag3 = 0;

	if( 1 == pixFlag ) /// 
	{
 		sprintf(showBuffer,"in 1++++++++++++++++++++\n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

        allFitFlag = 1;

		//gettimeofday(&start,NULL);

		if ( 1 == allFitFlag )
		{
			fitFlag1 = FittingChannelParas( pixValRGB[0], inlineFlagRGB, lineParas[0],inlinerRatio );

			if ( 0 == fitFlag1 )
			{
				allFitFlag = 0;
			}
			
		}

		if ( 1 == allFitFlag )
		{
			fitFlag2 = FittingChannelParas( pixValRGB[1], inlineFlagRGB, lineParas[1],inlinerRatio );

			if ( 0 == fitFlag2 )
			{
				allFitFlag = 0;
			}

		}

		if ( 1 == allFitFlag )
		{
			fitFlag3 = FittingChannelParas( pixValRGB[2], inlineFlagRGB, lineParas[2],inlinerRatio );

			if ( 0 == fitFlag3 )
			{
				allFitFlag = 0;
			}

		}




 		sprintf(showBuffer,"fit flag %d %d %d \n",fitFlag1,fitFlag2,fitFlag3 );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
 
 		sprintf(showBuffer,"r paras  %f %f %f\n",lineParas[0][0], lineParas[0][1],lineParas[0][2] );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
 		sprintf(showBuffer,"g paras  %f %f %f\n",lineParas[1][0], lineParas[1][1],lineParas[1][2] );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
 		sprintf(showBuffer,"b paras  %f %f %f\n",lineParas[2][0], lineParas[2][1],lineParas[2][2] );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		
		
		if ( 1 == allFitFlag )
		{
			

			//gettimeofday(&start,NULL);

			unsigned char *pTmp;
			for ( ind1 = 0; ind1 < bigHeight; ind1++ )
			{
				for ( ind3 = 0; ind3 < 3; ind3++ )
				{
					intVal = (int)( lineParas[ind3][0]*ind1*ind1+lineParas[ind3][1]*ind1+lineParas[ind3][2] + 0.5 );
					if ( intVal > 255 )
					{
						intVal = 255;
					}
					if ( intVal < 0 )
					{
						intVal = 0;
					}


					/*
					for (  ind2 = 0; ind2 < bigWidth; ind2++)
					{
					big[ind1*bigWidth*4+ind2*4+ind3] = ucPix;
					}
					*/

					pTmp = big+ind1*bigWidth*4+ind3;
					for ( ind2 = 0; ind2 < bigWidth; ind2++ )
					{
						*pTmp = intVal;
						pTmp += 4;
					}

				}

				/*
				for ( ind2 = 0; ind2 < bigWidth; ind2++ )
				{
				big[ind1*bigWidth*4+ind2*4+3] = 255;
				}
				*/

				pTmp = big+ind1*bigWidth*4+3;
				for ( ind2 = 0; ind2 < bigWidth; ind2++ )
				{
					*pTmp = 255;
					pTmp +=4;
				}



			}


			GetCenter( big,small, smallWidth, smallHeight,bigWidth, bandSize,acNum );

		}
		
	}
	
	if (  0 == pixFlag || 0 == allFitFlag )///
	{

 		sprintf(showBuffer,"in 2++++++++++++++++++++\n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );


		int blockSize = 0;
		int blockNum  = 0;

		blockSize = 32;
		blockNum = 256/blockSize;

		int allBlockNum = blockNum * blockNum * blockNum;

		int *indexFirst = new int[allBlockNum];

		for ( ind1 = 0; ind1 < allBlockNum; ind1++ )
		{
			indexFirst[ind1] = 0;
		}
	
        int shift = 0;

		int pix1 = 0;
		int pix2 = 0;
		int pix3 = 0;
		int temp = 0;

		int p1[3];
		int p2[3];
		int p3[3];

		

        
		shift = 0;
		temp = blockSize;
		while ( temp > 0 )
		{
			shift++;
			temp /= 2;
		}
		shift--;
		sprintf(showBuffer,"shift >>>>>>>>>>>>>>>>>>>>>>>> %d\n", shift );

		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
		for ( ind1 = 0; ind1 < smallHeight; ind1++ )
		{
			for ( ind2 = 0; ind2 < smallWidth; ind2++ )
			{
				temp = ind1*smallWidth*4+ind2*4;
				if ( 127 < small[temp+3] )
				{

					getHis( small[temp   ], shift,  16, p1 );
					getHis( small[temp +1], shift, 16, p2 );
					getHis( small[temp+2 ], shift, 16, p3 );

					

					for ( int i1 = 0; i1 < p1[2]; i1++ )
					{
						for ( int i2 = 0; i2 < p2[2]; i2++ )
						{
							for ( int i3 = 0; i3 < p3[2]; i3++ )
							{
								indexFirst[(p1[i1]*blockNum+p2[i2])*blockNum+p3[i3]]++;
							}

						}

					}
					
				}

			}

		}

		/*

		char *name = "//mnt//sdcard//indexdata.txt";
		remove(name);
		FILE *fp = fopen( name, "a" );
		for ( ind1 = 0; ind1 < allBlockNum ; ind1++ )
		{
			if ( 0 != indexFirst[ind1] )
			{
				fprintf( fp, "%d %d\n", ind1,indexFirst[ind1] );
			}
			

		}
		fclose( fp );
        */
    

		int maxIndex = 0;
		int maxNum  = 0;
		int tempSum = 0;


		/////allBlockNum 肯定大于sumSize*******

		int sumSize = 1;
		int halfSize = sumSize/2;
		maxNum = 0;
		for ( ind1 = 0; ind1 < sumSize; ind1++ )
		{
			maxNum += indexFirst[ind1];
		}
		
		tempSum = maxNum;
		for ( ind1 = halfSize+1; ind1 < allBlockNum-halfSize; ind1++ )
		{
			tempSum -= indexFirst[ind1-halfSize-1];
			tempSum += indexFirst[ind1+halfSize];

			if ( maxNum < tempSum )
			{
				maxNum = tempSum;
				maxIndex = ind1;
			}
			
		}
	    
	
		delete [] indexFirst;
		
		

 		sprintf(showBuffer,"color max index %d, %d\n", maxIndex,maxNum );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );


		unsigned char colorB = 0;
		unsigned char colorG = 0;
		unsigned char colorR = 0;

		colorB = maxIndex / blockNum / blockNum;
		colorG = maxIndex / blockNum % blockNum;
		colorR = maxIndex % blockNum;

 		sprintf(showBuffer,"init color res, %d %d %d\n",colorB, colorG, colorR );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );


		allBlockNum = blockSize*blockSize*blockSize;
		
 		sprintf(showBuffer,"bandNum %d  bandSize %d, allBlockNum %d \n", allBlockNum,blockSize,allBlockNum );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		int *indexLast = new int[allBlockNum];

		for ( ind1 = 0; ind1 < allBlockNum; ind1++ )
		{
			indexLast[ind1] = 0;
		}

		int b1,b2,g1,g2,r1,r2;

		b1 = colorB*blockSize;
		b2 = b1 + blockSize;

		g1 = colorG*blockSize;
		g2 = g1 + blockSize;

		r1 = colorR*blockSize;
		r2 = r1 + blockSize;

 		sprintf(showBuffer,"band size %d %d, %d %d, %d %d \n", b1, b2, g1, g2, r1, r2 );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
          
		/*
        char *name = "//mnt//sdcard//bug.txt";
		remove(name);
		FILE *fp = fopen( name, "a" );
        fprintf( fp, "band size %d %d, %d %d, %d %d \n", b1, b2, g1, g2, r1, r2 );
        fprintf( fp, "%d %d, %d\n", blockSize, blockNum, allBlockNum );
		*/
		
           
		int pixCounter = 0;         
		for ( ind1 = 0; ind1 < smallHeight; ind1++ )
		{
			for ( ind2 = 0; ind2 < smallWidth; ind2++ )
			{
				temp = ind1*smallWidth*4+ind2*4;
				if ( 0 != small[temp+3] )
				{				
					pixCounter++;
                                       
					if ( b1 <= small[temp+0] && small[temp+0] < b2 && g1 <= small[temp+1] && small[temp+1] < g2 && r1 <= small[temp+2] && small[temp+2] < r2 )
					{
                            pix1 = small[temp+0] - b1;
					        pix2 = small[temp+1] - g1;
					        pix3 = small[temp+2] - r1;
						    indexLast[(pix1*blockSize+pix2)*blockSize+pix3]++;

                            // fprintf( fp, "%d %d %d, %d %d %d \n", small[temp+0],small[temp+1],small[temp+2],pix1, pix2, pix3 );
                                     
					}
					
				}

			}

		}
		/*
		fclose(fp);

		char *aname = "//mnt//sdcard//last.txt";
		remove(aname);
		fp = fopen( aname, "a" );
		for( ind1 = 0; ind1 < allBlockNum; ind1++ )
		{
			if(indexLast[ind1]>0)
			{
				fprintf( fp, "%d %d\n",ind1, indexLast[ind1]);
			} 
		}
		fprintf( fp, "\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		for( ind1 = 0; ind1 < allBlockNum; ind1++ )
		{
			fprintf( fp, "%d %d\n",ind1, indexLast[ind1]);
		}
		fclose(fp);
        */     

 		sprintf(showBuffer,"counter ok \n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		/////allBlockNum 肯定大于sumSize*******
        
		

		for ( ind1 = 0; ind1 < sumSize; ind1++ )
		{
			maxNum += indexLast[ind1];
		}
 
		sprintf(showBuffer,"init0 ok ok \n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		tempSum = maxNum;
		for ( ind1 = halfSize+1; ind1 < allBlockNum-halfSize; ind1++ )
		{
			tempSum -= indexLast[ind1-halfSize-1];
			tempSum += indexLast[ind1+halfSize];

			if ( maxNum < tempSum )
			{
				maxNum = tempSum;
				maxIndex = ind1;
			}

		}

 		sprintf(showBuffer,"find max ok \n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		
		delete [] indexLast;

 		sprintf(showBuffer,"delete max ok \n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
		
		colorB = colorB*blockSize + maxIndex / blockSize / blockSize;
		colorG = colorG*blockSize + maxIndex / blockSize % blockSize;
		colorR = colorR*blockSize + maxIndex % blockSize;

 		sprintf(showBuffer,"color ok \n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );


		sprintf(showBuffer,"last ratio %f %d %d, maxindex %d\n", maxNum*1.0 / pixCounter, maxNum, pixCounter,maxIndex );
		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		
 		sprintf(showBuffer,"last color res, %d %d %d\n",colorB, colorG, colorR );
		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		
		/*
		for ( ind1 = 0; ind1 < bigHeight; ind1++ )
		{
			for ( ind2 = 0; ind2 < bigWidth; ind2++ )
			{
				big[ind1*bigWidth*4+ind2*4+0] = colorB;
				big[ind1*bigWidth*4+ind2*4+1] = colorG;
				big[ind1*bigWidth*4+ind2*4+2] = colorR;
				big[ind1*bigWidth*4+ind2*4+3] = 255;////////////都搞成不透明
			}
		}
		*/
	
		int pixNum = 0;
		unsigned char *ptemp;
		pixNum = bigWidth * bigHeight;
		ptemp = big;
		for ( ind1 = 0; ind1 < pixNum; ind1++ )
		{
			*ptemp++ = colorB;
			*ptemp++ = colorG;
			*ptemp++ = colorR;
			*ptemp++ = 255;

		}


		sprintf(showBuffer,"isize %d %d, %d %d  %d\n",smallWidth, smallHeight,bigWidth,bigHeight, bandSize );
		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		GetCenter( big,small, smallWidth, smallHeight,bigWidth, bandSize,acNum );


		
		
	}

	if (  1 == fitFlag1 && 1 == fitFlag2 && 1 == fitFlag3  )//1 == fitFlag1 && 1 == fitFlag2 && 1 == fitFlag3
	{

 		sprintf(showBuffer,"in 3++++++++++++++++++++\n" );
 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		int blockSize = 0;
		int blockNum  = 0;

		blockSize = 16;
		blockNum = 256/blockSize;

		int allBlockNum = blockNum * blockNum * blockNum;
		int *index = new int[allBlockNum];
		for ( ind1 = 0; ind1 < allBlockNum; ind1++ )
		{
			index[ind1] = 0;
		}

        int pixCounter = 0;

		int pix1 = 0;
		int pix2 = 0;
		int pix3 = 0;

		int shift = 0;
		int temp = blockSize;
		while ( temp > 0 )
		{
			shift++;
			temp /= 2;
		}
		shift--;

		for ( ind1 = 0; ind1 < smallHeight; ind1++ )
		{
			for ( ind2 = 0; ind2 < smallWidth; ind2++ )
			{
				temp = ind1*smallWidth*4+ind2*4;
				if ( 127 < small[temp+3] )
				{
					pixCounter++;
					pix1 = small[temp+0] >> shift;
					pix2 = small[temp+1] >> shift;
					pix3 = small[temp+2] >> shift;

					index[(pix1*blockNum+pix2)*blockNum+pix3]++;
				}

			}

		}

		
		int maxIndex = 0;
		int maxNum  = 0;
		int tempSum = 0;


		/////allBlockNum 肯定大于sumSize*******

		int sumSize = 3;
		int halfSize = sumSize/2;
		maxNum = 0;
		for ( ind1 = 0; ind1 < sumSize; ind1++ )
		{
			maxNum += index[ind1];
		}

		tempSum = maxNum;
		for ( ind1 = halfSize+1; ind1 < allBlockNum-halfSize; ind1++ )
		{
			tempSum -= index[ind1-halfSize-1];
			tempSum += index[ind1+halfSize];

			if ( maxNum < tempSum )
			{
				maxNum = tempSum;
				maxIndex = ind1;
			}

		}

		delete [] index;

		/////allBlockNum 肯定大于sumSize*******

		
// 		sprintf(showBuffer,"***************** %d\n", maxNum );
// 		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
// 		
// 
		sprintf(showBuffer,"ratio %f %d %d, %d %d %f\n", maxNum*1.0 / pixCounter, maxNum, pixCounter, smallWidth, smallHeight, pixCounter*1.0/(smallWidth*smallHeight) );
		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

		if ( maxNum*1.0 / pixCounter > 0.9 &&  pixCounter*1.0/(smallWidth*smallHeight) < 0.5 )/////////重要的判断标准
		{
			unsigned char colorB = 255;
			unsigned char colorG = 255;
			unsigned char colorR = 255;
            
			/*
			for ( ind1 = 0; ind1 < bigHeight; ind1++ )
			{
				for ( ind2 = 0; ind2 < bigWidth; ind2++ )
				{
					big[ind1*bigWidth*4+ind2*4+0] = colorB;
					big[ind1*bigWidth*4+ind2*4+1] = colorG;
					big[ind1*bigWidth*4+ind2*4+2] = colorR;
					big[ind1*bigWidth*4+ind2*4+3] = 255;////////////都搞成不透明
				}
			}
			*/

			int pixNum = 0;
			unsigned char *ptemp;
			pixNum = bigWidth * bigHeight;
			ptemp = big;
			for ( ind1 = 0; ind1 < pixNum; ind1++ )
			{
				*ptemp++ = colorB;
				*ptemp++ = colorG;
				*ptemp++ = colorR;
				*ptemp++ = 255;

			}


			GetCenter( big,small, smallWidth, smallHeight,bigWidth, bandSize,acNum );

 
 			sprintf(showBuffer,"color res, %d %d %d\n",colorB, colorG, colorR );
 			__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
		}

	
		
	}
	
	
	if ( 1 == initflag )
	{
		LineFree();

		sprintf(showBuffer,"free ok\n" );
		__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
	}

	
    
    return 1;

}
