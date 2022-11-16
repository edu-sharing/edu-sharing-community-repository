import { Injectable } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class RocketChatService {
    opened = false;
    unread = 0;
    _data: any;
}
