#ifndef QUADRATICCURVEFITTING_H	
#define	QUADRATICCURVEFITTING_H



extern int **pixValRGB;

extern float **lineParas;


extern int  *inlineFlagRGB;

int LineInit( int _bandSize );
void LineFree();
int GetPixVal( unsigned char *imageData, int width, int height, int widthSetp, int nChannels);
int FittingChannelParas( int *pixVals, int *flag, float *paras,float inlinerRatio);

#endif