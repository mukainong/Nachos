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

  for(i=0;i<5;i++) {
	for(j=0; j < 100; j++);
  }

  Exit(1);
}

void foo()
{
  int i, j;
  
  Sleep(2000);

  for(i=0;i<7;i++) {
	for(j=0; j < 100; j++);
  }

  Exit(2);
}

void foo1()
{
  int i, j;

  Sleep(3000);

  for(i=0;i<8;i++) {
	for(j=0; j < 100; j++);
  }
  
  Exit(3);
}