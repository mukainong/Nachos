
#include "syscall.h"

int main()
{
  int i,j;

  for(i=0;i<10;i++) {
	for(j=0; j < 100; j++);
    Write("Timesharing 3\r\n",15,ConsoleOutput);
  }
  
  Exit(8);
}
