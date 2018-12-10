#include <string.h>
#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <fcntl.h>
#include <deque>
#include <vector>
#include <sys/time.h>

int main(int argc, char* argv[]);

static int fdFromChild = -1;
static int fdToChild = -1;

/*
 * Class:     org_empyrn_darkknight_engine_NativePipedProcess
 * Method:    startProcess
 * Signature: ()V
 */
/*
  * 클래스 : org_empyrn_darkknight_engine_NativePipedProcess
  * 메소드 : startProcess
  * 서명 : () V
 */
extern "C" JNIEXPORT void JNICALL Java_org_empyrn_darkknight_engine_NativePipedProcess_startProcess
		(JNIEnv* env, jobject obj)
{
	int fd1[2];		/* parent -> child */
    int fd2[2];		/* child -> parent */
    if (pipe(fd1) < 0)
    	exit(1);
    if (pipe(fd2) < 0)
    	exit(1);
    int childpid = fork();
    if (childpid == -1) {
        exit(1);
    }
    if (childpid == 0) {
    	close(fd1[1]);
    	close(fd2[0]);
    	close(0); dup(fd1[0]); close(fd1[0]);
    	close(1); dup(fd2[1]); close(fd2[1]);
    	close(2); dup(1);
    	static char* argv[] = {"stockfish", NULL};
    	nice(5);
   	    main(1, argv);
    	_exit(0);
    } else {
    	close(fd1[0]);
    	close(fd2[1]);
    	fdFromChild = fd2[0];
    	fdToChild = fd1[1];
	    fcntl(fdFromChild, F_SETFL, O_NONBLOCK);
    }
}


static std::deque<char> inBuf;

static bool getNextChar(int& c, int timeoutMillis) {
	if (inBuf.empty()) {
	    fd_set readfds, writefds;
		FD_ZERO(&readfds);
		FD_SET(fdFromChild, &readfds);
		struct timeval tv;
		tv.tv_sec = timeoutMillis / 1000;
		tv.tv_usec = (timeoutMillis % 1000) * 1000;
		int ret = select(fdFromChild + 1, &readfds, NULL, NULL, &tv);
		if (ret < 0)
			return false;

		static char buf[4096];
		int len = read(fdFromChild, &buf[0], sizeof(buf));
		for (int i = 0; i < len; i++)
			inBuf.push_back(buf[i]);
	}
	if (inBuf.empty()) {
		c = -1;
		return true;
	}
	c = inBuf.front();
	inBuf.pop_front();
	return true;
}

static std::vector<char> lineBuf;
/*
 * Class:     org_empyrn_darkknight_engine_NativePipedProcess
 * Method:    readFromProcess
 * Signature: (I)Ljava/lang/String;
 */
/*
  * 클래스 : org_empyrn_darkknight_engine_NativePipedProcess
  * 메서드 : readFromProcess
  * 서명 : (I) Ljava / lang / String;
 */
extern "C" JNIEXPORT jstring JNICALL Java_org_empyrn_darkknight_engine_NativePipedProcess_readFromProcess
		(JNIEnv* env, jobject obj, jint timeoutMillis)
{
	struct timeval tv0, tv1;
	while (true) {
		int c;
		gettimeofday(&tv0, NULL);
		if (!getNextChar(c, timeoutMillis))
			return 0; // Error
		gettimeofday(&tv1, NULL);
		int elapsedMillis = (tv1.tv_sec - tv0.tv_sec) * 1000 + (tv1.tv_usec - tv0.tv_usec) / 1000;
		if (elapsedMillis > 0) {
			timeoutMillis -= elapsedMillis;
			if (timeoutMillis < 0) timeoutMillis = 0;
		}
		if (c == -1) { // Timeout
			static char emptyString = 0;
			return (*env).NewStringUTF(&emptyString);
		}
		if (c == '\n' || (c == '\r')) {
			if (lineBuf.size() > 0) {
				lineBuf.push_back(0);
				jstring ret = (*env).NewStringUTF(&lineBuf[0]);
				lineBuf.clear();
				return ret;
			}
		} else {
			lineBuf.push_back((char)c);
		}
	}
}

/*
 * Class:     org_empyrn_darkknight_engine_NativePipedProcess
 * Method:    writeToProcess
 * Signature: (Ljava/lang/String;)V
 */
/*
  * 클래스 : org_empyrn_darkknight_engine_NativePipedProcess
  * 메서드 : writeToProcess
  * 서명 : (Ljava / lang / String;) V
 */
extern "C" JNIEXPORT void JNICALL Java_org_empyrn_darkknight_engine_NativePipedProcess_writeToProcess
		(JNIEnv* env, jobject obj, jstring msg)
{
    const char* str = (*env).GetStringUTFChars(msg, NULL);
    if (str) {
    	int len = strlen(str);
    	int written = 0;
    	while (written < len) {
    		int n = write(fdToChild, &str[written], len - written);
    		if (n <= 0)
    			break;
    		written += n;
    	}
    	(*env).ReleaseStringUTFChars(msg, str);
    }
}
