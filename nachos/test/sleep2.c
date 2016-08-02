/* Basic test of Fork() system call */

#include "syscall.h"

int
main()
{
  int i, j;

  Sleep(500);

  for(i=0;i<5;i++) {
	for(j=0; j < 100; j++);
  }

  Exit(1);
}