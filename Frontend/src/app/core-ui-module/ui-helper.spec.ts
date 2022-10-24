import { fakeAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import {UIHelper} from './ui-helper';

describe('Test UI Helper', () => {
    describe('Test password strength', () => {
        it('should be weak password', async () => {
            expect(UIHelper.getPasswordStrengthString('abc')).toBe('weak');
            expect(UIHelper.getPasswordStrengthString('abcd')).toBe('weak');
            expect(UIHelper.getPasswordStrengthString('abc2')).toBe('weak');
        });
        it('should be accept password', async () => {
            expect(UIHelper.getPasswordStrengthString('abcde')).toBe('accept');
            expect(UIHelper.getPasswordStrengthString('abcde2')).toBe('accept');
            expect(UIHelper.getPasswordStrengthString('abcdef2')).toBe('accept');
        });
        it('should be medium password', async () => {
            expect(UIHelper.getPasswordStrengthString('ABCabc123')).toBe('medium');
            expect(UIHelper.getPasswordStrengthString('ABCabc12345')).toBe('medium');
            expect(UIHelper.getPasswordStrengthString('ABCabc#!$')).toBe('medium');
        });
        it('should be strong password', async () => {
            expect(UIHelper.getPasswordStrengthString('ABCabc123#1!')).toBe('strong');
            expect(UIHelper.getPasswordStrengthString('ABCabc12345#23')).toBe('strong');
            expect(UIHelper.getPasswordStrengthString('ABCabcABCabcABCabcABCabc')).toBe('strong');
        });
    });
});
