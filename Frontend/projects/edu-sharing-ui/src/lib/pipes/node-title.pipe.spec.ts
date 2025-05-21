import { TranslateService } from '@ngx-translate/core';
import { NodeTitlePipe } from './node-title.pipe';
import { Node, RestConstants } from 'ngx-edu-sharing-api';

const translateServiceMock = {
    instant: (key: string, values?: any) => key,
} as TranslateService;

describe('NodeTitlePipe', () => {
    let pipe = new NodeTitlePipe(translateServiceMock);

    it('should create the pipe', () => {
        expect(pipe).toBeTruthy();
    });
    it('should return the translation', () => {
        let result = pipe.transform('HOME', { type: 'name' });
        expect(result).toBe('WORKSPACE.HOME');
        result = pipe.transform('HOME', { type: 'title' });
        expect(result).toBe('WORKSPACE.HOME');
    });
    it('should return the name', () => {
        let result = pipe.transform(
            {
                name: 'Test Name',
                title: 'Test Title',
            } as Node,
            { type: 'name' },
        );
        expect(result).toBe('Test Name');
        result = pipe.transform(
            {
                properties: {
                    [RestConstants.CM_NAME]: ['Test Name'],
                },
            } as Node,
            { type: 'name' },
        );
        expect(result).toBe('Test Name');
    });
    it('should return the title', () => {
        let result = pipe.transform(
            {
                name: 'Test Name',
                title: 'Test Title',
            } as Node,
            { type: 'title' },
        );
        expect(result).toBe('Test Title');
        result = pipe.transform(
            {
                properties: {
                    [RestConstants.LOM_PROP_TITLE]: ['Test Title'],
                },
            } as Node,
            { type: 'title' },
        );
        expect(result).toBe('Test Title');
        result = pipe.transform(
            {
                name: 'Test Name',
            } as Node,
            { type: 'title' },
        );
        expect(result).toBe('Test Name');
        result = pipe.transform(
            {
                properties: {
                    [RestConstants.CM_NAME]: ['Test Title'],
                },
            } as Node,
            { type: 'title' },
        );
        expect(result).toBe('Test Title');
        result = pipe.transform(
            {
                properties: {
                    [RestConstants.CM_NAME]: 'Test Title',
                },
            } as unknown as Node,
            { type: 'title' },
        );
        expect(result).toBe('Test Title');
    });
});
