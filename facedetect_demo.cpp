#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/opencv.hpp"

#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <linux/kernel.h>
#include <unistd.h>
#include <cctype>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdio.h>
#include <vector>
#include <string.h>
#include <typeinfo>

// --------------------------------------------------------------
// ★raspicam対応
// --------------------------------------------------------------
#include "RaspiCamCV.h"
#include "liblazurite.h"

using namespace std;
using namespace cv;
using namespace lazurite;

Mat detectFaceInImage(Mat &image,string &cascade_file,int *num){
	CascadeClassifier cascade;
	cascade.load(cascade_file);

	vector<Rect> faces;
	cascade.detectMultiScale(image, faces, 1.1,3,0,Size(20,20));

	*num = faces.size();

	for (int i = 0; i < faces.size(); i++){
		rectangle(image, Point(faces[i].x,faces[i].y),Point(faces[i].x + faces[i].width,faces[i].y + faces[i].height),Scalar(0,200,0),3,CV_AA);
	}

	return image;
}

int main( int argc, const char** argv )
{
	// check args
	int result;
	char *en;

	// check args
	uint8_t ch=60;
	uint16_t panid=0xFFFF;
	uint16_t rxaddr=0xFFFF;
	int ack;
	if(argc > 1) {
		ch = strtol(argv[1],&en,0);
	}
	if(argc > 2) {
		panid = strtol(argv[2],&en,0);
	}
	if(argc > 3){
		rxaddr = strtol(argv[3],&en,0);
	}
	printf("ch = %d, panid=%04x, rxaddr=%04x\n",ch,panid,rxaddr);

	if((result=lazurite_init())!=0) {
		printf("lazurite_open error = %d",result);
		return EXIT_FAILURE;
	}
	if((result=lazurite_begin(ch,panid,100,20))!=0) {
		printf("lazurite_setPanid error = %d",result);
		return EXIT_FAILURE;
	}

	//CvCapture* capture = 0;
	RaspiCamCvCapture* capture = 0;

	RASPIVID_CONFIG * config = (RASPIVID_CONFIG*)malloc(sizeof(RASPIVID_CONFIG));
	config->width=320;
	config->height=240;
	config->bitrate=0;  // zero: leave as default
	config->framerate=0;
	config->monochrome=0;

	// save 
	//VideoWriter writer("result.avi",CV_FOURCC_DEFAULT,10,Size(640,480),true);

	// --------------------------------------------------------------
	cv::Mat frame1,grayImage;

	// catch raspi-cam
	capture = (RaspiCamCvCapture *) raspiCamCvCreateCameraCapture2(0, config); 

	//cvNamedWindow( "drawing", 1 );
	cvNamedWindow( "origin", 1 );

	//cvSetMouseCallback("origin",on_mouse,0);

	if( capture )
	{
		cout << "In capture ..." << endl;
		for(;;)
		{

			// -------------------------------------------
			// ★raspicam対応
			// -------------------------------------------
			//IplImage* iplImg = cvQueryFrame( capture );
			IplImage* iplImg1;
			iplImg1 = raspiCamCvQueryFrame( capture );
			frame1 = iplImg1;
			char msg[64];
			int numFace;

			// optimize frame
			cv::flip(frame1,frame1,0);

			// face detect
			string filename = "/home/pi/opencv/data/haarcascades/haarcascade_frontalface_default.xml";
			frame1=detectFaceInImage(frame1,filename,&numFace);

			if(numFace > 0){
				// gen motor power
				sprintf(msg,"%d",numFace*50);
				cout << msg << endl;

				// display log
				ack= lazurite_send(panid,rxaddr,msg,strlen(msg));
				if(ack <0 ) {
					lazurite_close();
					lazurite_remove();
					lazurite_init();
					lazurite_begin(ch,panid,100,20);
					printf("restert lzgw\n");
				}
			}
			// send log through sub-ghz

			// display Image
			cv::imshow("origin", frame1);

			if( waitKey( 10 ) >= 0 )
				goto _cleanup_;
		}

		waitKey(0);

_cleanup_:

		// -------------------------------------------
		// ★raspicam対応
		// -------------------------------------------
		//cvReleaseCapture( &capture );
		raspiCamCvReleaseCapture(&capture);
	}
	//cvDestroyWindow("drawing");
	cvDestroyWindow("origin");

	lazurite_close();
	lazurite_remove();

	return 0;
}

