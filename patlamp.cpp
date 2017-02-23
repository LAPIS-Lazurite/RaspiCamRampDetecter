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
#include <time.h>

// --------------------------------------------------------------
// ★raspicam対応
// --------------------------------------------------------------
#include "RaspiCamCV.h"
#include "liblazurite.h"

using namespace std;
using namespace cv;
using namespace lazurite;
typedef struct {
	uint8_t r;
	uint8_t g;
	uint8_t b;
} color_table;

RNG rng;
struct MOUSE_POINTER{
	int x;
	int y;
} mouse ={0,0};

typedef struct {
	String name;
	String color;
	int x;
	int y;
	int size;
	int threshold;
} MACHINE_MAP;

vector <MACHINE_MAP> pat_ramp;

int ramp_count;
int cycle_count;
int detected_count[128];
unsigned short myaddr;

void load_mapfile()
{
	static fstream fs_map;
	int offset_x = 0;
	int offset_y = 0;
	char *en;
	string token;

	MACHINE_MAP mmap;

	// clear vector
	pat_ramp.clear();
	// open mapfile
	fs_map.open("/home/pi/patlamp/map.txt",std::ios::in);
	if(!fs_map.is_open()){
		return;
	}
	string reading_buffer;

	// get 1st line as offset
	while(!fs_map.eof())
	{
		MACHINE_MAP mmap;
		istringstream stream(reading_buffer);

		getline(fs_map, reading_buffer);

		getline(stream,token,',');
		mmap.name = token;
		getline(stream,token,',');
		mmap.color = token;
		getline(stream,token,',');
		mmap.x=strtol(token.c_str(),&en,10)+offset_x;
		getline(stream,token,',');
		mmap.y=strtol(token.c_str(),&en,10)+offset_y;
		getline(stream,token,',');
		mmap.size=strtol(token.c_str(),&en,10);
		getline(stream,token,',');
		mmap.threshold=strtol(token.c_str(),&en,10);

		if(mmap.name == "offset")
		{
			offset_x = mmap.x;
			offset_y = mmap.y;
		}
		else if(mmap.name.substr(0,1)!="#") pat_ramp.push_back(mmap);
		//cout << reading_buffer << endl;
	}
	fs_map.close();
	pat_ramp.erase(pat_ramp.begin());
}

void on_mouse(int event,int x, int y,int flags,void *param=NULL){
	switch(event)
	{
		case CV_EVENT_MOUSEMOVE:
			mouse.x =x;
			mouse.y =y;
			break;
	}
}

void serchBlinking(Mat grayImage, Mat &cameraFeed){
	// Initialization
	Mat temp;
	vector< vector<Point> > contours;
	vector<Vec4i> hierarchy;

	// get map file
	load_mapfile();

	// buffer to print text
	char str[64];
	char mousec[64];

	// for area of change
	Rect rect;
	Scalar color;

	// initializing log
	//log = "";
	// copy original image
	cameraFeed.copyTo(temp);

	// detect of changes
	Scalar mean;

	ramp_count = pat_ramp.size();

	for(int i = 0; i < pat_ramp.size();i++) {
		//		minEnclosingCircle((Mat)contours[i],center[i],radius[i]);
		// cut image
		rect = Rect(pat_ramp[i].x-pat_ramp[i].size/2,
				pat_ramp[i].y-pat_ramp[i].size/2,
				pat_ramp[i].size,
				pat_ramp[i].size);
		Mat blinkArea(grayImage,rect);
		// averaging color
		mean = cv::mean(blinkArea);

		// generate color from mean and map
		if(mean[0]>pat_ramp[i].threshold){
			detected_count[i]++;
			//log += "1,";
			if(pat_ramp[i].color=="g") color=Scalar(0,255,0);
			else if(pat_ramp[i].color=="y") color=Scalar(0,255,255);
			else if(pat_ramp[i].color=="r") color=Scalar(0,0,255);
			else color = Scalar(255,255,255);
		} else {
			color = Scalar(255,255,255);
			//log += "0,";
		}

		// generating line in frame
		line(cameraFeed,Point(rect.x             ,rect.y             ),Point(rect.x + rect.width,rect.y             ),color,0.1);
		line(cameraFeed,Point(rect.x             ,rect.y             ),Point(rect.x             ,rect.y + rect.width),color,0.1);
		line(cameraFeed,Point(rect.x + rect.width,rect.y             ),Point(rect.x + rect.width,rect.y + rect.width),color,0.1);
		line(cameraFeed,Point(rect.x             ,rect.y + rect.width),Point(rect.x + rect.width,rect.y + rect.width),color,0.1);

		// generating strings for frame
		sprintf(str,"%d,%s,%d", i,pat_ramp[i].name.c_str(),(int)mean[0]);

		// put text
		if(rect.x<320){
			putText(cameraFeed,str,
					cv::Point(rect.x,rect.y),
					FONT_HERSHEY_TRIPLEX,0.5,color,0.1,CV_AA);
		} else {
			putText(cameraFeed,str,
					cv::Point(rect.x-50,rect.y),
					FONT_HERSHEY_TRIPLEX,0.5,color,0.1,CV_AA);
		}
	}
	sprintf(mousec,"%d,%d", mouse.x,mouse.y);
	putText(cameraFeed,mousec,
			cv::Point(10,100),
			FONT_HERSHEY_TRIPLEX,1,CV_RGB(255,255,255),1,CV_AA);

	return;
}

int main( int argc, const char** argv )
{
	// check args
	int result;
	char *en;

	// check args
	uint8_t ch=36;
	uint16_t panid=0xFFFF;
	uint16_t rxaddr=0xFFFF;

	// time
	time_t current_time;
	time_t last_tx_time;
	time(&last_tx_time);
	cycle_count = 0;
	ramp_count = 0;
	memset(detected_count,0,sizeof(detected_count));
	
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
	if((result=lazurite_setAddrType(4)!=0)) {
		printf("lazurite_setPanid error = %d",result);
		return EXIT_FAILURE;
	}
	if((result=lazurite_begin(ch,panid,100,20))!=0) {
		printf("lazurite_setPanid error = %d",result);
		return EXIT_FAILURE;
	}

	lazurite_getMyAddress(&myaddr);
	printf("myaddress = 0x%04x\n",myaddr);

	//CvCapture* capture = 0;
	RaspiCamCvCapture* capture = 0;

	RASPIVID_CONFIG * config = (RASPIVID_CONFIG*)malloc(sizeof(RASPIVID_CONFIG));
	config->width=640;
	config->height=480;
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

	cvSetMouseCallback("origin",on_mouse,0);

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

			// optimize frame
			cv::flip(frame1,frame1,0);

			// convert to grayImage
			cv::cvtColor(frame1,grayImage,COLOR_BGR2GRAY);

			serchBlinking(grayImage,frame1);
			cycle_count++;

			// check tx timing
			time(&current_time);
			if((current_time - last_tx_time)>10)
			{
				char result1[16];
				char result2[16];
				char result3[250];
				char result4[250];
				memset(result3,0,sizeof(result3));
				last_tx_time=current_time;
				sprintf(result1,"0x%04x",myaddr);
				for(int i = 0;i<ramp_count;i++) {
					sprintf(result2,",%.1f",(float)detected_count[i]/cycle_count*100);
					strcat(result3,result2);
				}
				sprintf(result4,"%s%s\n",result1,result3);
				printf("%s",result4);
				int ack;
				// display log
				ack= lazurite_send(panid,rxaddr,result4,strlen(result4));
				if(ack <0 ) {
					lazurite_close();
					lazurite_remove();
					lazurite_init();
					lazurite_setAddrType(4);
					lazurite_begin(ch,panid,100,20);
					printf("restert lzgw\n");
				}
				// send log through sub-ghz
				cycle_count = 0;
				memset(detected_count,0,sizeof(detected_count));
			}


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

