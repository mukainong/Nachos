/* Basic test of Fork() system call */

#include "syscall.h"

void foo();
void foo1();

int
main()
{
  int i, j;

  Fork(&foo1);
  Fork(&foo);

  for(i=0;i<4;i++) {
	for(j=0; j < 5; j++);
  }

  Exit(1);
}

void foo()
{
  int i, j;
  
  for(i=0;i<5;i++) {
	for(j=0; j < 10; j++);
		Sleep(200);
  }

  Exit(2);
}

void foo1()
{
  int i, j;

  for(i=0;i<3;i++) {
	for(j=0; j < 10; j++);
		Sleep(300);
  }
  
  Exit(3);
}