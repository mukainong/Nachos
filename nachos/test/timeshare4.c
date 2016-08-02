
#include "syscall.h"

int main()
{
  int i;

  for(i=0;i<10;i++) {
    Write("Timesharing 4\r\n",15,ConsoleOutput);
  }
  
  Exit(9);
}
