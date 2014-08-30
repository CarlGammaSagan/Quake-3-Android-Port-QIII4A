/*
 * Android audio code for Quake3
 * Copyright (C) 2010 Roderick Colenbrander
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdbool.h>

#include "../client/client.h"
#include "../client/snd_local.h"
#include "../sys/sys_local.h"

static int buf_size=0;
static int bytes_per_sample=0;
static int chunkSizeBytes=0;
static int dmapos=0;

static float cursor_x=0, cursor_y=0;

static qboolean motion_event = qfalse;
static float motion_dx=0;
static float motion_dy=0;

static qboolean trackball_event = qfalse;
static float trackball_dx = 0;
static float trackball_dy = 0;

static float scale_ratio;
static int offset_x;

void (*setState)(int shown);

void (*initAudio)(void *buffer, int size);
void (*writeAudio)(int offset, int length);

qboolean SNDDMA_Init(void)
{
    Com_Printf("Initializing Android Sound subsystem\n");

    /* For now hardcode this all :) */
    dma.channels = 2;
    dma.samples = 32768;
    dma.samplebits = 16;

    dma.submission_chunk = 4096; /* This is in single samples, so this would equal 2048 frames (assuming stereo) in Android terminology */
    dma.speed = 44100; /* This is the native sample frequency of the Milestone */

    bytes_per_sample = dma.samplebits/8;
    buf_size = dma.samples * bytes_per_sample;
    dma.buffer = calloc(1, buf_size);

    chunkSizeBytes = dma.submission_chunk * bytes_per_sample;

    initAudio(dma.buffer, buf_size);

    return qtrue;
}


int SNDDMA_GetDMAPos(void)
{
    return dmapos;
}

void SNDDMA_Shutdown(void)
{
    Com_Printf("SNDDMA_ShutDown\n");
}

void SNDDMA_BeginPainting (void)
{
}

void Q3E_SetCallbacks(void *init_audio, void *write_audio, void *set_state)
{
    setState=set_state;
    writeAudio = write_audio;
    initAudio = init_audio;
}

void Q3E_GetAudio(void)
{
    int offset = (dmapos * bytes_per_sample) & (buf_size - 1);
    writeAudio(offset, chunkSizeBytes);
    dmapos+=dma.submission_chunk;
}

extern void IN_Frame();
extern bool scndswp;
void Q3E_DrawFrame()
{
	scndswp=0;
	IN_Frame( );
	Com_Frame( );
}

void SNDDMA_Submit(void)
{
}

void setInputCallbacks(void *set_state)
{
    setState = set_state;
}

#define MAX_CONSOLE_KEYS 16

/*
===============
IN_IsConsoleKey
===============
*/
static qboolean IN_IsConsoleKey( keyNum_t key, const unsigned char character )
{
	typedef struct consoleKey_s
	{
		enum
		{
			KEY,
			CHARACTER
		} type;

		union
		{
			keyNum_t key;
			unsigned char character;
		} u;
	} consoleKey_t;

	static consoleKey_t consoleKeys[ MAX_CONSOLE_KEYS ];
	static int numConsoleKeys = 0;
	int i;

	// Only parse the variable when it changes
	if( cl_consoleKeys->modified )
	{
		char *text_p, *token;

		cl_consoleKeys->modified = qfalse;
		text_p = cl_consoleKeys->string;
		numConsoleKeys = 0;

		while( numConsoleKeys < MAX_CONSOLE_KEYS )
		{
			consoleKey_t *c = &consoleKeys[ numConsoleKeys ];
			int charCode = 0;

			token = COM_Parse( &text_p );
			if( !token[ 0 ] )
				break;

			if( strlen( token ) == 4 )
				charCode = Com_HexStrToInt( token );

			if( charCode > 0 )
			{
				c->type = CHARACTER;
				c->u.character = (unsigned char)charCode;
			}
			else
			{
				c->type = KEY;
				c->u.key = Key_StringToKeynum( token );

				// 0 isn't a key
				if( c->u.key <= 0 )
					continue;
			}

			numConsoleKeys++;
		}
	}

	// If the character is the same as the key, prefer the character
	if( key == character )
		key = 0;

	for( i = 0; i < numConsoleKeys; i++ )
	{
		consoleKey_t *c = &consoleKeys[ i ];

		switch( c->type )
		{
			case KEY:
				if( key && c->u.key == key )
					return qtrue;
				break;

			case CHARACTER:
				if( c->u.character == character )
					return qtrue;
				break;
		}
	}

	return qfalse;
}

void Q3E_KeyEvent(int state,int key,int character)
{
    int t = Sys_Milliseconds();
    if ((state==1)&&(IN_IsConsoleKey(key,character)))
    {
	Com_QueueEvent(t, SE_KEY, K_CONSOLE, state, 0, NULL);
	return;
    }
    if (key!=0)
    {
    Com_QueueEvent(t, SE_KEY, key, state, 0, NULL);
    Com_DPrintf("SE_KEY key=%d state=%d\n", key, state);
    }
    if ((state==1)&&(character!=0))
    {
        Com_DPrintf("SE_CHAR key=%d state=%d\n", character, state);
        Com_QueueEvent(t, SE_CHAR, character, 0, 0, NULL);
    }
}

float analogx=0.0f;
float analogy=0.0f;
int analogenabled=0;
void Q3E_Analog(int enable,float x,float y)
{
analogenabled=enable;
analogx=x;
analogy=y;
}

enum Action
{
    ACTION_DOWN=0,
    ACTION_UP=1,
    ACTION_MOVE=2
};


void Q3E_MotionEvent(float dx, float dy)
{
     motion_event = qtrue;
     motion_dx += dx;
     motion_dy += dy;
}

static void processMotionEvents(void)
{
    if(motion_event)
    {
        Com_QueueEvent(0, SE_MOUSE, (int)(motion_dx), (int)(motion_dy), 0, NULL);
        motion_event = qfalse;
        motion_dx = 0;
        motion_dy = 0;
    }
}

void queueTrackballEvent(int action, float dx, float dy)
{
    static int keyPress=0;

    switch(action)
    {
        case ACTION_DOWN:
            Com_QueueEvent(0, SE_KEY, K_MOUSE1, 1, 0, NULL);
            keyPress=1;
            break;
        case ACTION_UP:
            if(keyPress) Com_QueueEvent(0, SE_KEY, K_MOUSE1, 0, 0, NULL);
            keyPress=0;
    }

    trackball_dx += dx;
    trackball_dy += dy;
    trackball_event = qtrue;
}

static void processTrackballEvents(void)
{
    if(trackball_event)
    {
        /* Trackball dx/dy are <1.0, so make them a bit bigger to prevent kilometers of scrolling */
        trackball_dx *= 50.0;
        trackball_dy *= 50.0;
        Com_QueueEvent(0, SE_MOUSE, (int)trackball_dx, (int)trackball_dy, 0, NULL);

        trackball_event = qfalse;
        trackball_dx = 0;
        trackball_dy = 0;
    }
}

void IN_Frame(void)
{
    static int prev_state = -1;
    static int state = -1;
    processMotionEvents();
    processTrackballEvents();

    state = ((clc.state == CA_ACTIVE) && (Key_GetCatcher() == 0)) << 1;

    if (state != prev_state)
    {
        setState(state);
        prev_state = state;
    }
}

void IN_Init(void)
{
}

/*qboolean IN_MotionPressed(void)
{
    return qfalse;
}*/

void IN_Shutdown(void)
{
}

/*void Sys_SendKeyEvents (void) {
}*/

void IN_Restart(void)
{
    IN_Init();
}

