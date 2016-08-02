/* Basic test of Fork() system call */

#include "syscall.h"

void foo();
void foo1();

int globalInt = 5;

int
main()
{
  int i, j;

  Fork(&foo1);
  Fork(&foo);

  for(i=0;i<5;i++) {
	for(j=0; j < 100; j++);
    Write("Timesharing 7\r\n",15,ConsoleOutput);
  }

  Exit(1);
}

void foo()
{
  int i, j;

  for(i=0;i<10;i++) {
	for(j=0; j < 100; j++);
    Write("Timesharing 8\r\n",15,ConsoleOutput);
  }

  Exit(2);
}

void foo1()
{
  int i, j;

  for(i=0;i<5;i++) {
	for(j=0; j < 100; j++);
    Write("Timesharing 9\r\n",15,ConsoleOutput);
  }
  
  Exit(3);
}