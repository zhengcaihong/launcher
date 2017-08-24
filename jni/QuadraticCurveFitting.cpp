#include "QuadraticCurveFitting.h"
#include "stdlib.h"
#include "stdio.h"
#include "LinearAlgebraic.h"
#include "math.h"
#include <memory.h>

#include "RandomIndex.h"
#include <android/log.h>
#include <sys/time.h>
#include <string.h>

static char showBuffer[256];
static float timeuse;

int **pixValRGB = NULL;
float **lineParas = NULL;

#define MAX_POINTS_NUM 512

int  *inlineFlagRGB;

int *refineInliersFlag;

int cutSize = 40;
int bandSize = 64;

int pointsNum = 0;


float **a3p = NULL;
float  *b3p = NULL;
int  *index3p;


float **asvd = NULL;
float  *wsvd = NULL;
float **vsvd = NULL;
float  *bsvd = NULL;

void LineFree();

int LineInit( int _bandSize )
{

	sprintf(showBuffer,"start line initalize" );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	int ind = 0;

	pixValRGB = ( int ** )malloc( sizeof( int * ) * 3 );
        memset(pixValRGB, 0, sizeof( int * ) * 3);
	if ( NULL == pixValRGB )
	{
		LineFree();
		return 0;
	}

	lineParas = ( float ** )malloc( sizeof( float * ) * 3 );
        memset(lineParas, 0,  sizeof(float * ) * 3 );
	if ( NULL == lineParas )
	{
		LineFree();
		return 0;
	}

	for ( ind = 0; ind < 3; ind++ )
	{
		pixValRGB[ind] = ( int * )malloc( sizeof( int * ) * MAX_POINTS_NUM );
		if ( NULL == pixValRGB[ind] )
		{
			LineFree();
			return 0;
		}


		lineParas[ind] = ( float * )malloc( sizeof( float ) * 3 );
		if ( NULL == lineParas[ind] )
		{
			LineFree();
			return 0;
		}

	}

	inlineFlagRGB   = ( int * )malloc( sizeof(int * ) * MAX_POINTS_NUM );
	if ( NULL == inlineFlagRGB )
	{
		LineFree();
		return 0;
	}

	refineInliersFlag = ( int * )malloc( sizeof(int * ) * MAX_POINTS_NUM );
	if ( NULL == refineInliersFlag )
	{
		LineFree();
		return 0;
	}

	bandSize = _bandSize;


	a3p       = dmatrix( 1 ,3, 1,3 );
	if ( NULL == a3p )
	{
		LineFree();
		return 0;
	}

	b3p       = dvector( 1, 3 );
	if ( NULL == b3p )
	{
		LineFree();
		return 0;
	}

	index3p   = ivector( 1, 3 );
	if ( NULL == index3p )
	{
		LineFree();
		return 0;
	}

	asvd = dmatrix( 1 ,MAX_POINTS_NUM, 1,4 );
	if ( NULL == asvd )
	{
		LineFree();
		return 0;
	}

	wsvd = dvector( 1, 4 );
	if ( NULL == wsvd )
	{
		LineFree();
		return 0;
	}

	vsvd = dmatrix( 1,4,1,4);
	if ( NULL == vsvd )
	{
		LineFree();
		return 0;
	}

	bsvd = dvector( 1, MAX_POINTS_NUM );
	if ( NULL == bsvd )
	{
		LineFree();
		return 0;
	}



	sprintf(showBuffer,"line init ok" );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );


	return 1;


}

void LineFree()
{
	int ind = 0;

	
	for ( ind =0; ind < 3; ind++ )
	{
		if ( NULL != pixValRGB[ind] )
		{
			free( pixValRGB[ind] );
			pixValRGB[ind] = NULL;
		}
		if ( NULL != lineParas[ind] )
		{
			free( lineParas[ind] );
			lineParas[ind] = NULL;
		}
		
	}
	if ( NULL != pixValRGB )
	{
		free( pixValRGB );pixValRGB = NULL;
	}
	
	if ( NULL != lineParas )
	{
		free( lineParas );lineParas=NULL;
	}
	

	if ( NULL != inlineFlagRGB )
	{
		free( inlineFlagRGB   );inlineFlagRGB = NULL;
	}
	
	if ( NULL != refineInliersFlag )
	{
		free( refineInliersFlag );refineInliersFlag = NULL;
	}
	

	if ( NULL != a3p )
	{
		free_dmatrix( a3p, 1 ,3, 1,3 );a3p = NULL;
	}
	if ( NULL != b3p )
	{
		free_dvector( b3p, 1 ,3 );b3p =NULL;
	}

	if ( NULL != index3p )
	{
		free_ivector( index3p, 1 ,3 );index3p = NULL;
	}
	
	
	


	if ( NULL != asvd )
	{
		free_dmatrix( asvd, 1, MAX_POINTS_NUM, 1, 4 );asvd = NULL;
	}

	if ( NULL != wsvd )
	{
		free_dvector( wsvd, 1, 4       );wsvd = NULL;
	}

	if ( NULL != vsvd )
	{
		free_dmatrix( vsvd, 1, 4, 1, 4 ); vsvd =NULL;
	}

	if ( NULL != bsvd )
	{
		free_dvector( bsvd, 1, MAX_POINTS_NUM       );bsvd = NULL;
	}

	
	
	
	


}

int GetPixVal( unsigned char *imageData, int width, int height, int widthSetp, int nChannels )////cutsize
{
	

	sprintf(showBuffer,"GetPixVal %d %d, %d %d \n",width,height,widthSetp,nChannels );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	int ind1 = 0;
	int ind2 = 0;

	int maxNum = 0;
	int maxIndex = 0;
	int accNum = 0;


	int temp = 0;;
	for( ind1 = 0; ind1 < width; ind1++ )
	{
		accNum = 0;
		temp = ind1*nChannels+3;
		for(ind2 = 0; ind2 < height; ind2++ )
		{
			if( 0!= imageData[temp] )//ind2*widthSetp+ind1*nChannels+3
			{
				accNum++;
			}
			temp += widthSetp;
		}
		if( accNum > maxNum )
		{
			maxNum=accNum;
			maxIndex = ind1;
		}
	}
	



	 	sprintf(showBuffer,"size  %d %d, %d %d \n",height, width, maxNum, maxIndex );
	   __android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	if( maxNum < 30 )
	{
		return 0;
	}
	else
	{
		for( ind1 = 0; ind1 < height; ind1++ )
		{
			if( 0 != imageData[ind1*widthSetp+maxIndex*nChannels+3] )
			{
				cutSize = ind1;/////////////cutSize 这里赋值
				break;
			}
		}

 
		sprintf(showBuffer,"cutsize  %d  %d, maxIndex %d *******\n",cutSize,maxNum, maxIndex );
	   __android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	   for( int index = 0; index < width - maxIndex - 1; index++ )
	   {
		   int sum1 = 0;
		   int sum2 = 0;
		   int sum3 = 0;

		   maxIndex += index;

		   pointsNum = 0;
	       for( ind1 = 0; ind1 < height; ind1++ )
		  {
			  if( 0 != imageData[ind1*widthSetp+maxIndex*nChannels+3] )
			   {
				   temp = ind1*widthSetp+maxIndex*nChannels+0;
				   pixValRGB[0][pointsNum] = imageData[temp  ];
				   pixValRGB[1][pointsNum] = imageData[temp+1];
				   pixValRGB[2][pointsNum] = imageData[temp+2];
				   sum1 += pixValRGB[0][pointsNum];
				   sum2 += pixValRGB[1][pointsNum];
				   sum3 += pixValRGB[2][pointsNum];
				   pointsNum++;
				   ////比较强的判断
				   if( pointsNum >= MAX_POINTS_NUM )
				   {
					   break;
				   }
				
			   }
		  }
		  if( 0 == sum1 && 0 == sum2 && 0 == sum3 )
		  {
			  continue;
		  }
		  else
		  {
			  break;
		  }

	   }
	  

	sprintf(showBuffer,"pointsNum  %d  \n",pointsNum );
	 __android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
	}

	return 1;
}


void Get3RandomIndices( int n_max, int &n1, int &n2, int &n3 )
{
	n1 = rand() % n_max;
	do n2 = rand() % n_max; while(n2 == n1);
	do n3 = rand() % n_max; while(n3 == n1 || n3 == n2);
}

void GetQuadraticParaFrom3Points( int *pixVals, int step, int *pointsIndex, float *paras )
{
	int ind1 = 0;
	int ind2 = 0;

	int size = 3;

	
	float d = 0;

	int x = 0;

	for ( ind1 = 0; ind1 < size; ind1++ )
	{
		x = pointsIndex[ind1]+step;
		a3p[ind1+1][1] = x * x;
		a3p[ind1+1][2] = x;
		a3p[ind1+1][3] = 1;

		b3p[ind1+1] = pixVals[pointsIndex[ind1]];

	}


	
	ludcmp( a3p, size, index3p, &d );
	lubksb( a3p, size, index3p, b3p );
	

	for ( ind1 = 0; ind1 < 3; ind1++ )
	{
		paras[ind1] = b3p[ind1+1];
	}

}


int ComputerInliers( int *pixVals, int *flag, float *paras, float threshold, int step )
{
	int ind = 0;
	int inliersCounter = 0;
	float pixThu = threshold*threshold;

	int x = 0;
    float val = 0;

	float a = paras[0];
	float b = paras[1];
	float c = paras[2];

	inliersCounter = 0;

	for ( ind = 0; ind < pointsNum; ind++ )
	{
		x = step + ind;
		val = a*x*x + b*x + c - pixVals[ind];

		if ( val*val > pixThu )
		{
			flag[ind] = 0;
		}
		else
		{
			flag[ind] = 1;
			inliersCounter++;

		}
	}

	return inliersCounter;

}


int ComputerInliersTest( int *pixVals, int *flag, float *paras, float threshold, int step )
{
	int ind = 0;
	int inliersCounter = 0;
	float pixThu = threshold*threshold;

	int x = 0;
	float val = 0;

	float a = paras[0];
	float b = paras[1];
	float c = paras[2];

	inliersCounter = 0;

	for ( ind = 0; ind < pointsNum; ind++ )
	{
		x = step + ind;
		val = a*x*x + b*x + c - pixVals[ind];

		if ( val*val > pixThu )
		{
			flag[ind] = 0;
		}
		else
		{
			flag[ind] = 1;
			inliersCounter++;

		}
	}

	return inliersCounter;

}



void SVDRefine( int *pixVals, float *paras, int *flag, int step, int inlinerNum )
{
	int ind1 = 0;
	int ind2 = 0;


	int m = inlinerNum;
	int n = 4;


	sprintf(showBuffer,"svd refine %d \n",inlinerNum );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );
 


	int counter = 1;
    int x = 0;
	counter = 1;
	for ( ind1 = 0; ind1 < pointsNum; ind1++ )
	{
		if ( 1 == flag[ind1] )
		{
			x = ind1 + step;
			asvd[counter][1] = x*x;
			asvd[counter][2] = x;
			asvd[counter][3] = 1;
			asvd[counter][4] = -pixVals[ind1];

			counter++;
		}
	}

	sprintf(showBuffer,"svd refine couter %d %d \n",inlinerNum, counter );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	int index = 0;
	float tmp = 0.0;

	svdcmp( asvd, m,n,wsvd,vsvd );

	tmp = wsvd[1];
	index = 1;
	for ( int ind = 2; ind <= n; ind++ )///////////////这个svd分解并不彻底，要找到相应的列，也就是对应的w最小的位置
	{
		
		if ( tmp > wsvd[ind] )
		{
			tmp = wsvd[ind];
			index = ind;
		}
	}

	float temp[4];
	for ( int ind = 0; ind < 4; ind++ )
	{
		temp[ind] = vsvd[ind+1][index];
	}



	for ( int ind = 0; ind < 3; ind++  )
	{
		paras[ind] = temp[ind]/temp[3];
	}

}
int FittingChannelParas( int *pixVals, int *flag, float *paras, float inlinerRatio )
{
	if ( pointsNum < 30 )//////更强的保护
	{
		return 0;
	}

	int ind1 = 0;
	int ind2 = 0;
	
	int step = cutSize + bandSize;//////////

	int pointsIndex[3] = { 0 };

	int   tempInlinerNum = 0;
	float tempRatio = 0;

	float tempParas[3] = { 0 };

	float stopRatio = 0.6;///////////////
	int ransacNum = 30;///////////////
	int pixThresohld = 5;

	int ransacCounter = 0;

	int maxNum = 0;
	float bestParas[3];

	ransacCounter = 0;
	tempRatio = 0;
	maxNum = 0;
	while ( ransacCounter < ransacNum && tempRatio < stopRatio )
	{
		ransacCounter++;

        if ( pointsNum <= MAX_RANDOM_INDEX_NUM ) {
            pointsIndex[0] = randomIndex[pointsNum*(ransacNum-1)*3+(ransacCounter-1)*3+0];
            pointsIndex[1] = randomIndex[pointsNum*(ransacNum-1)*3+(ransacCounter-1)*3+1];
            pointsIndex[2] = randomIndex[pointsNum*(ransacNum-1)*3+(ransacCounter-1)*3+2];
        } else {
            Get3RandomIndices( pointsNum, pointsIndex[0], pointsIndex[1], pointsIndex[2]);
        }
		GetQuadraticParaFrom3Points( pixVals, step, pointsIndex, tempParas );

		tempInlinerNum = ComputerInliers( pixVals, flag, tempParas, pixThresohld, step );
		tempRatio = tempInlinerNum*1.0 / pointsNum;

		if ( tempInlinerNum > maxNum )
		{
			maxNum = tempInlinerNum;
			bestParas[0] = tempParas[0];
			bestParas[1] = tempParas[1];
			bestParas[2] = tempParas[2];

			for ( ind1 = 0; ind1 < pointsNum; ind1++ )
			{
				refineInliersFlag[ind1] = flag[ind1];
			}


		}

	}
 



	
	int refineNum = 3;
	int refineCounter = 0;
	float errorEsp = 0.05;///////////////百分之5的误差
	float lastRatio = 0;
	int refineFlag = 1;

	refineCounter = 0;

	lastRatio = maxNum*1.0/pointsNum;
         sprintf(showBuffer,"maxNum %d %d \n",maxNum,pointsNum );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	float eps = 0.00001;

	if ( fabs( bestParas[0] ) < eps && fabs( bestParas[1] ) < eps  && fabs( bestParas[2] ) < eps  )///////////svd分解出问题
	{
		refineFlag = 0;
	}


	tempInlinerNum = maxNum;
	while ( refineCounter < refineNum && refineFlag )
	{
	
		////加上更深层的保护
		if( tempInlinerNum < 10 )
		{
			return 0;
		}


		SVDRefine(  pixVals, bestParas, refineInliersFlag, step, tempInlinerNum );

		tempInlinerNum = ComputerInliers( pixVals, refineInliersFlag, bestParas, pixThresohld, step );
		tempRatio = tempInlinerNum*1.0 / pointsNum;


		if ( fabs(lastRatio-tempRatio) < errorEsp )
		{
			refineFlag = 0;
		}

		lastRatio = tempRatio;

 		refineCounter++;
	}
   
	paras[0] = bestParas[0];
	paras[1] = bestParas[1];
	paras[2] = bestParas[2];

	
	sprintf(showBuffer,"inliner ratio %f %f \n",lastRatio,inlinerRatio );
	__android_log_write(ANDROID_LOG_DEBUG,"icongenerator",showBuffer );

	if( lastRatio < inlinerRatio )///////重要的阈值
	{
		return 0;
	}else
	{
		return 1;
	}

}
