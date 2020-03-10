//Using SDL, standard IO, and strings
#include <SDL2/SDL.h>
#include <SDL2/SDL_mixer.h>
#include <SDL2/SDL_ttf.h>
#include <stdio.h>
#include <string>
#include <ctime>
#include <cstdlib>
#include <cstring>
#include "note.h"
#include "note_image.h"
#include "scene.h"
#include "music.h"
#include "const.h"
#include "lane.h"
#include "beatmap.h"
#include "score.h"
#include "secret.h"

//Starts up SDL and creates window
bool init();

//Frees media and shuts down SDL
void close();

//Loads individual image
SDL_Surface* loadSurface( const char * path );

//The window we'll be rendering to
SDL_Window* gWindow = NULL;

SDL_Renderer* gRenderer = NULL;

bool loadImages();
SDL_Texture *arrowImage;
ArrowImage arrows[4];

TTF_Font *mainFont = NULL;

Scene *mainScene = NULL;
Music *music = NULL;
Score *score;
std::vector<Lane *> lanes;

char song[50];
char song_path[100];
char map_path[100];
char secret_path[100];

bool finished() {
	for(auto lane : lanes) {
		if(!lane->finished) return false;
	}
	return true;
}

bool init()
{
	//Initialization flag
	bool success = true;

	//Initialize SDL
	if( SDL_Init( SDL_INIT_VIDEO | SDL_INIT_AUDIO ) < 0 )
	{
		printf( "SDL could not initialize! SDL Error: %s\n", SDL_GetError() );
		success = false;
	}
	else
	{
		//Create window
		gWindow = SDL_CreateWindow( "UTCTF Rhythm Game", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, SCREEN_WIDTH, SCREEN_HEIGHT, SDL_WINDOW_SHOWN );
		if( gWindow == NULL )
		{
			printf( "Window could not be created! SDL Error: %s\n", SDL_GetError() );
			success = false;
		}
		else
		{
			//Create renderer for window
			gRenderer = SDL_CreateRenderer( gWindow, -1, SDL_RENDERER_ACCELERATED );
			if( gRenderer == NULL )
			{
				printf( "Renderer could not be created! SDL Error: %s\n", SDL_GetError() );
				success = false;
			}
			else
			{
				if( Mix_OpenAudio( 44100, MIX_DEFAULT_FORMAT, 2, 2048 ) < 0 )
                {
                    printf( "SDL_mixer could not initialize! SDL_mixer Error: %s\n", Mix_GetError() );
                    success = false;
                }

				TTF_Init();
				if(!loadImages()){
					success = false;
				}

				//Initialize renderer color
				SDL_SetRenderDrawColor( gRenderer, 0xFF, 0xFF, 0xFF, 0xFF );

				//create scene
				mainScene = new Scene(gRenderer);

				//create music
				music = new Music(song_path);

				score = new Score(gRenderer, mainFont);
				mainScene->push_back(score);

				//test lane
				for(int i = 0; i < 4; i++){
					Lane *newLane = new Lane(gRenderer, music, arrows[i], 100 + 110 * i, score);
					mainScene->push_back(newLane);
					lanes.push_back(newLane);
				}
			}
		}
	}

	return success;
}

void close()
{
	//Destroy window
	SDL_DestroyWindow( gWindow );
	gWindow = NULL;

	delete music;
	music = NULL;

	TTF_CloseFont(mainFont);
	mainFont = NULL;

	TTF_Quit();

	//Quit SDL subsystems
	SDL_Quit();
}

SDL_Surface* loadSurface( const char * path )
{
	//Load image at specified path
	SDL_Surface* loadedSurface = SDL_LoadBMP( path );
	if( loadedSurface == NULL )
	{
		printf( "Unable to load image %s! SDL Error: %s\n", path, SDL_GetError() );
	}

	return loadedSurface;
}

bool loadImages()
{
	bool success = true;
	
	SDL_Surface *surface = loadSurface(ARROW_PATH);
	if(surface == NULL){
		success = false;
	}else{
		SDL_SetColorKey(surface, SDL_TRUE, SDL_MapRGB( surface->format, 0, 0, 0));
		for(int i = 0; i < 4; i++){
			arrows[i].image = SDL_CreateTextureFromSurface(gRenderer, surface);
			SDL_SetTextureBlendMode(arrows[i].image, SDL_BLENDMODE_BLEND);
		}
		arrows[0].rotation = 270;
		arrows[1].rotation = 180;
		arrows[2].rotation = 0;
		arrows[3].rotation = 90;
		arrows[0].rgb = 0xe0607e;
		arrows[1].rgb = 0xc2714f;
		arrows[2].rgb = 0x7e9181;
		arrows[3].rgb = 0x016fb9;
	}

	mainFont = TTF_OpenFont(FONT_PATH, 30);
	if(mainFont == NULL){
		printf("Failed to load font: %s\n", TTF_GetError());
		success = false;
	}

	return success;
}

//loop for creating a beatmap
void create_loop(const char *path) {
	//Main loop flag
	bool quit = false;

	//Event handler
	SDL_Event e;

	music->play();

	std::vector<std::vector<Note *>> lanes(4, std::vector<Note *>());

	bool crazy = false;
	double prevNoteTime = 0;

	//While application is running
	while( !quit )
	{
		//Handle events on queue
		while( SDL_PollEvent( &e ) != 0 )
		{
			//User requests quit
			if( e.type == SDL_QUIT )
			{
				quit = true;
			}
			//User presses a key
			else if( e.type == SDL_KEYDOWN )
			{
				int laneNo = -1;
				switch( e.key.keysym.sym )
				{
					case SDLK_w:
					case SDLK_UP: laneNo = 2; break;
					case SDLK_s:
					case SDLK_DOWN: laneNo = 1; break;
					case SDLK_a:
					case SDLK_LEFT: laneNo = 0; break;
					case SDLK_d:
					case SDLK_RIGHT: laneNo = 3; break;
					case SDLK_SPACE: crazy = true; break;
					default: break;
				}

				double now = music->getSeconds() * (SLOW? 0.5: 1);
				if(laneNo != -1) {
					lanes[laneNo].push_back(new Note(now));
					printf("%lf\n", now);
				}
				if(crazy){
					if(now > prevNoteTime) {
						int rand1 = rand() % 4;
						int rand2 = rand() % 3;
						if(rand2 >= rand1) rand2++;
						lanes[rand1].push_back(new Note(now));
						lanes[rand2].push_back(new Note(now));
						printf("%lf\n", now);
					}
					prevNoteTime = now;
				}
			}else if(e.type == SDL_KEYUP) {
				if(e.key.keysym.sym == SDLK_SPACE){
					crazy = false;
				}
			}
		}

		SDL_SetRenderDrawColor(gRenderer, 0xFF, 0xFF, 0xFF, 0xFF);
		SDL_RenderClear(gRenderer);

		mainScene->render();

		//Update screen
		SDL_RenderPresent( gRenderer );
	}

	writeBeatMap(path, lanes);
}

bool allPerfect() {
	return score->counts[AMAZING] == 0 && score->counts[GREAT] == 0 && score->counts[BAD] == 0;
}

void game_loop() {
	//Main loop flag
	bool quit = false;
	bool shownEndBox = false;

	//Event handler
	SDL_Event e;

	music->play();

	//While application is running
	while( !quit )
	{
		//Handle events on queue
		while( SDL_PollEvent( &e ) != 0 )
		{
			//User requests quit
			if( e.type == SDL_QUIT )
			{
				quit = true;
			}
			//User presses a key
			else if( e.type == SDL_KEYDOWN )
			{
				int laneNo = -1;
				switch( e.key.keysym.sym )
				{
					case SDLK_w:
					case SDLK_UP: laneNo = 2; break;
					case SDLK_s:
					case SDLK_DOWN: laneNo = 1; break;
					case SDLK_a:
					case SDLK_LEFT: laneNo = 0; break;
					case SDLK_d:
					case SDLK_RIGHT: laneNo = 3; break;
					default: break;
				}
				if(laneNo != -1) {
					lanes[laneNo]->hit();
				}
			}
		}

		if(CHEAT){
			for(int i = 0; i < 4; i++){
				lanes[i]->hit();
			}
		}

		SDL_SetRenderDrawColor(gRenderer, 0xFF, 0xFF, 0xFF, 0xFF);
		SDL_RenderClear(gRenderer);

		for(auto lane : lanes) {
			lane->updateViewable();
		}
		mainScene->render();

		//Update screen
		SDL_RenderPresent( gRenderer );

		if(!shownEndBox && finished()) {

			if(!allPerfect()){
				SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_INFORMATION, "Done", "Get all perfect to decrypt the secret!", gWindow);
			}else if(!showSecret(gWindow, lanes, secret_path)){
				SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_INFORMATION, "Done", "There seems to be no secret attached to this song, or the secret was corrupted. ", gWindow);
			}

			shownEndBox = true;
		}
	}
}

int main( int argc, char* argv[] )
{
	strcpy(song, "clutterfunk");
	if(argc == 2) {
		snprintf(song, 50, "%s", argv[1]);
	}

	snprintf(song_path, 100, "assets/%s.ogg", song);
	snprintf(map_path, 100, "assets/%s.map", song);
	snprintf(secret_path, 100, "assets/%s.secret", song);

	srand(time(NULL));
	//Start up SDL and create window
	if( !init() )
	{
		printf( "Failed to initialize!\n" );
	}
	else
	{
		if(CREATE){
			create_loop(map_path);
		}else{
			readBeatMap(map_path, lanes);
			game_loop();
		}
	}

	//Free resources and close SDL
	close();

	return 0;
}