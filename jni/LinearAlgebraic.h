#ifndef LINEARALGEBRAIC_H_
#define LINEARALGEBRAIC_H_

#define NR_END 1
#define FREE_ARG char*
#define TINY 1.0e-20
#define SIGN(a,b) ((b) >= 0.0 ? fabs(a) : -fabs(a))
static float dmaxarg1, dmaxarg2;
#define DMAX(a,b) (dmaxarg1=(a),dmaxarg2=(b),(dmaxarg1) > (dmaxarg2) ?\
	(dmaxarg1) : (dmaxarg2))
static int iminarg1, iminarg2;
#define IMIN(a,b) (iminarg1=(a),iminarg2=(b),(iminarg1) < (iminarg2) ?\
	(iminarg1) : (iminarg2))

int *ivector(long nl, long nh);
float **dmatrix(int nrl, int nrh, int ncl, int nch);
float *dvector(int nl, int nh);

float pythag(float a, float b);

void free_dmatrix(float **m, int nrl, int nrh, int ncl, int nch);
void free_dvector(float *v, int nl, int nh);
void free_ivector(int *v, int nl, int nh);

void ludcmp(float **a, int n, int *indx, float *d);
void lubksb(float **a, int n, int *indx, float b[]);
void svdcmp(float **a, int m, int n, float w[], float **v);
void svbksb(float **u, float w[], float **v, int m, int n, float b[],
		float x[]);
#endif
